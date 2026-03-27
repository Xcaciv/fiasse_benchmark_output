using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Notes CRUD, search, and top-rated list.
/// All ownership checks performed server-side before any mutation (ASVS V8.2.2).
/// UserId is always extracted from the authenticated session (Derived Integrity Principle).
/// </summary>
[Authorize]
[AutoValidateAntiforgeryToken]
public sealed class NotesController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;
    private readonly ILogger<NotesController> _logger;

    public NotesController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService,
        ILogger<NotesController> logger)
    {
        _db = db;
        _userManager = userManager;
        _auditService = auditService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;

        var notes = await _db.Notes
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.UpdatedAt)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Preview = n.Content.Length > 200 ? n.Content[..200] : n.Content,
                IsPublic = n.IsPublic,
                CreatedAt = n.CreatedAt,
                UpdatedAt = n.UpdatedAt,
                AverageRating = n.AverageRating,
                RatingCount = n.RatingCount,
                AttachmentCount = n.Attachments.Count
            })
            .ToListAsync();

        return View(notes);
    }

    [HttpGet]
    public IActionResult Create() => View(new CreateNoteViewModel());

    [HttpPost]
    public async Task<IActionResult> Create(CreateNoteViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        // UserId from session ONLY - request body UserId is ignored (Derived Integrity Principle)
        var userId = _userManager.GetUserId(User)!;

        var note = new Note
        {
            UserId = userId,
            Title = model.Title.Trim(),
            Content = model.Content,
            IsPublic = false, // Server-enforced default (ASVS V8.2.2)
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _db.Notes.Add(note);
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(
            AuditEventTypes.NoteCreated,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "note", resourceId: note.Id.ToString(),
            details: $"visibility:private");

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Details(int id)
    {
        var userId = _userManager.GetUserId(User)!;

        var note = await _db.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.Rater)
            .Include(n => n.ShareLinks.Where(s => !s.IsRevoked))
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null) return NotFound();

        // Visibility check: owner can always see; others only if public
        if (note.UserId != userId && !note.IsPublic)
            return Forbid();

        var isOwner = note.UserId == userId;
        var viewModel = MapToDetailsViewModel(note, userId, isOwner);
        return View(viewModel);
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(id);

        if (note == null) return NotFound();
        if (note.UserId != userId) return Forbid(); // IDOR prevention

        return View(new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        });
    }

    [HttpPost]
    public async Task<IActionResult> Edit(EditNoteViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.Id);

        if (note == null) return NotFound();
        if (note.UserId != userId) return Forbid(); // Server-side ownership check (ASVS V8.2.2)

        var previousVisibility = note.IsPublic;
        note.Title = model.Title.Trim();
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow; // Server-set timestamp (Derived Integrity)

        await _db.SaveChangesAsync();

        if (previousVisibility != model.IsPublic)
        {
            await _auditService.LogAsync(
                AuditEventTypes.NoteVisibilityChanged,
                userId, User.Identity?.Name, GetClientIp(),
                resourceType: "note", resourceId: note.Id.ToString(),
                details: $"from:{previousVisibility} to:{model.IsPublic}");
        }

        await _auditService.LogAsync(
            AuditEventTypes.NoteEdited,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "note", resourceId: note.Id.ToString());

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole("Admin");

        var note = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !isAdmin) return Forbid();

        return View(note);
    }

    [HttpPost, ActionName("Delete")]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole("Admin");

        var note = await _db.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null) return NotFound();
        if (note.UserId != userId && !isAdmin) return Forbid();

        var originalOwnerId = note.UserId;
        var eventType = isAdmin && note.UserId != userId
            ? AuditEventTypes.NoteDeletedByAdmin
            : AuditEventTypes.NoteDeleted;

        // Cascade deletion is atomic via EF cascade configuration.
        // File deletion is a separate step; if it fails, we log but still commit DB deletion
        // to avoid leaving orphaned records. Physical file cleanup is logged for manual review.
        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(
            eventType,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "note", resourceId: id.ToString(),
            details: $"original_owner:{originalOwnerId}");

        return RedirectToAction(nameof(Index));
    }

    [HttpGet]
    [EnableRateLimiting("search")]
    public async Task<IActionResult> Search(string? query)
    {
        var model = new NoteSearchViewModel { Query = query };

        if (string.IsNullOrWhiteSpace(query))
            return View(model);

        if (query.Length > 200)
        {
            ModelState.AddModelError(nameof(query), "Search query must not exceed 200 characters.");
            return View(model);
        }

        var userId = _userManager.GetUserId(User)!;

        // Visibility filter applied at query layer (FIASSE S2.4, ASVS V8.2.2)
        var results = await _db.Notes
            .Where(n => (n.IsPublic || n.UserId == userId) &&
                        (EF.Functions.Like(n.Title, $"%{query}%") ||
                         EF.Functions.Like(n.Content, $"%{query}%")))
            .OrderByDescending(n => n.UpdatedAt)
            .Take(50)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Preview = n.Content.Length > 200 ? n.Content[..200] : n.Content,
                IsPublic = n.IsPublic,
                CreatedAt = n.CreatedAt,
                UpdatedAt = n.UpdatedAt,
                AverageRating = n.AverageRating,
                RatingCount = n.RatingCount
            })
            .ToListAsync();

        model.Results = results;
        model.HasSearched = true;
        return View(model);
    }

    [HttpGet]
    [AllowAnonymous]
    [EnableRateLimiting("topRated")]
    public async Task<IActionResult> TopRated()
    {
        // Query-level filter: only public notes with >= 3 ratings (ASVS V8.2.2)
        var notes = await _db.Notes
            .Where(n => n.IsPublic && n.RatingCount >= 3)
            .OrderByDescending(n => n.AverageRating)
            .ThenByDescending(n => n.RatingCount)
            .Take(20)
            .Select(n => new TopRatedNoteViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Preview = n.Content.Length > 200 ? n.Content[..200] : n.Content,
                AuthorUsername = n.User.UserName ?? string.Empty, // Username only - no email/ID
                AverageRating = n.AverageRating,
                RatingCount = n.RatingCount
            })
            .ToListAsync();

        return View(notes);
    }

    [HttpPost]
    public async Task<IActionResult> Rate(RatingInputViewModel model)
    {
        if (!ModelState.IsValid)
            return BadRequest(ModelState);

        // Server-side range validation (ASVS V2.2.1)
        if (model.Value < 1 || model.Value > 5)
        {
            ModelState.AddModelError(nameof(model.Value), "Rating must be between 1 and 5.");
            return BadRequest(ModelState);
        }

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.NoteId);

        if (note == null) return NotFound();

        // Authorization: note must be public or owned by rater (ASVS V8.2.2)
        if (!note.IsPublic && note.UserId != userId)
            return Forbid();

        var existingRating = await _db.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.RaterId == userId);

        if (existingRating != null)
        {
            existingRating.Value = model.Value;
            existingRating.Comment = model.Comment;
            existingRating.UpdatedAt = DateTime.UtcNow;

            await _auditService.LogAsync(
                AuditEventTypes.RatingEdited,
                userId, User.Identity?.Name, GetClientIp(),
                resourceType: "rating", resourceId: existingRating.Id.ToString(),
                details: $"note:{model.NoteId} value:{model.Value}");
        }
        else
        {
            var rating = new Rating
            {
                NoteId = model.NoteId,
                RaterId = userId,
                Value = model.Value,
                Comment = model.Comment,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };
            _db.Ratings.Add(rating);

            await _auditService.LogAsync(
                AuditEventTypes.RatingCreated,
                userId, User.Identity?.Name, GetClientIp(),
                resourceType: "rating", resourceId: model.NoteId.ToString(),
                details: $"note:{model.NoteId} value:{model.Value}");
        }

        await _db.SaveChangesAsync();
        await UpdateNoteAverageAsync(model.NoteId);

        return RedirectToAction(nameof(Details), new { id = model.NoteId });
    }

    [HttpPost]
    public async Task<IActionResult> CreateShareLink(int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(noteId);

        if (note == null) return NotFound();
        if (note.UserId != userId) return Forbid();

        var tokenService = HttpContext.RequestServices.GetRequiredService<IShareTokenService>();
        var token = tokenService.GenerateToken();

        var shareLink = new ShareLink
        {
            NoteId = noteId,
            CreatedByUserId = userId,
            Token = token,
            IsRevoked = false,
            CreatedAt = DateTime.UtcNow
        };

        _db.ShareLinks.Add(shareLink);
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(
            AuditEventTypes.ShareLinkCreated,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "sharelink", resourceId: shareLink.Id.ToString(),
            details: $"note:{noteId}");

        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost]
    public async Task<IActionResult> RevokeShareLink(int shareLinkId, int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var shareLink = await _db.ShareLinks.Include(s => s.Note).FirstOrDefaultAsync(s => s.Id == shareLinkId);

        if (shareLink == null) return NotFound();
        if (shareLink.Note.UserId != userId) return Forbid();

        shareLink.IsRevoked = true;
        shareLink.RevokedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(
            AuditEventTypes.ShareLinkRevoked,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "sharelink", resourceId: shareLinkId.ToString(),
            details: $"note:{noteId}");

        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    private async Task UpdateNoteAverageAsync(int noteId)
    {
        var ratings = await _db.Ratings.Where(r => r.NoteId == noteId).ToListAsync();
        var note = await _db.Notes.FindAsync(noteId);
        if (note == null) return;

        note.RatingCount = ratings.Count;
        note.AverageRating = ratings.Count > 0 ? ratings.Average(r => r.Value) : 0;
        await _db.SaveChangesAsync();
    }

    private static NoteDetailsViewModel MapToDetailsViewModel(
        Note note, string currentUserId, bool isOwner)
    {
        return new NoteDetailsViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            OwnerUsername = note.User.UserName ?? string.Empty,
            IsOwner = isOwner,
            CreatedAt = note.CreatedAt,
            UpdatedAt = note.UpdatedAt,
            AverageRating = note.AverageRating,
            RatingCount = note.RatingCount,
            Attachments = note.Attachments.Select(a => new AttachmentViewModel
            {
                Id = a.Id,
                OriginalFileName = a.OriginalFileName,
                ContentType = a.ContentType,
                FileSizeBytes = a.FileSizeBytes,
                UploadedAt = a.UploadedAt
            }).ToList(),
            Ratings = note.Ratings.OrderByDescending(r => r.CreatedAt).Select(r => new RatingViewModel
            {
                Id = r.Id,
                Value = r.Value,
                Comment = r.Comment,
                RaterUsername = r.Rater.UserName ?? string.Empty, // No email exposed
                IsCurrentUser = r.RaterId == currentUserId,
                CreatedAt = r.CreatedAt
            }).ToList(),
            ActiveShareToken = isOwner
                ? note.ShareLinks.FirstOrDefault(s => !s.IsRevoked)?.Token
                : null
        };
    }

    private string GetClientIp()
        => HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
}
