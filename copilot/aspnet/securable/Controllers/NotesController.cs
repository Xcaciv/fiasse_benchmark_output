using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;

namespace LooseNotes.Controllers;

public sealed class NotesController : Controller
{
    private readonly ApplicationDbContext _dbContext;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IFileStorageService _fileStorageService;
    private readonly IShareLinkService _shareLinkService;
    private readonly IActivityLogService _activityLogService;

    public NotesController(
        ApplicationDbContext dbContext,
        UserManager<ApplicationUser> userManager,
        IFileStorageService fileStorageService,
        IShareLinkService shareLinkService,
        IActivityLogService activityLogService)
    {
        _dbContext = dbContext;
        _userManager = userManager;
        _fileStorageService = fileStorageService;
        _shareLinkService = shareLinkService;
        _activityLogService = activityLogService;
    }

    [Authorize]
    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken cancellationToken)
    {
        var userId = _userManager.GetUserId(User)!;
        var notes = await _dbContext.Notes
            .AsNoTracking()
            .Where(x => x.OwnerId == userId)
            .OrderByDescending(x => x.UpdatedAtUtc)
            .Select(x => new NoteCardViewModel
            {
                Id = x.Id,
                Title = x.Title,
                Excerpt = x.Content.Length > 200 ? x.Content.Substring(0, 200) + "..." : x.Content,
                OwnerUserName = x.Owner.UserName ?? "Unknown",
                CreatedAtUtc = x.CreatedAtUtc,
                UpdatedAtUtc = x.UpdatedAtUtc,
                IsPublic = x.IsPublic,
                AverageRating = x.Ratings.Any() ? x.Ratings.Average(r => r.Value) : 0,
                RatingCount = x.Ratings.Count
            })
            .ToListAsync(cancellationToken);

        return View(notes);
    }

    [HttpGet]
    public async Task<IActionResult> Search(string? query, CancellationToken cancellationToken)
    {
        var normalizedQuery = query?.Trim();
        var userId = User.Identity?.IsAuthenticated == true ? _userManager.GetUserId(User) : null;
        var notesQuery = _dbContext.Notes
            .AsNoTracking()
            .Where(x => x.IsPublic || x.OwnerId == userId);

        if (!string.IsNullOrWhiteSpace(normalizedQuery))
        {
            var pattern = $"%{normalizedQuery}%";
            notesQuery = notesQuery.Where(x => EF.Functions.Like(x.Title, pattern) || EF.Functions.Like(x.Content, pattern));
        }

        var results = await notesQuery
            .OrderByDescending(x => x.UpdatedAtUtc)
            .Select(x => new NoteCardViewModel
            {
                Id = x.Id,
                Title = x.Title,
                Excerpt = x.Content.Length > 200 ? x.Content.Substring(0, 200) + "..." : x.Content,
                OwnerUserName = x.Owner.UserName ?? "Unknown",
                CreatedAtUtc = x.CreatedAtUtc,
                UpdatedAtUtc = x.UpdatedAtUtc,
                IsPublic = x.IsPublic,
                AverageRating = x.Ratings.Any() ? x.Ratings.Average(r => r.Value) : 0,
                RatingCount = x.Ratings.Count
            })
            .ToListAsync(cancellationToken);

        return View(new SearchViewModel { Query = normalizedQuery, Results = results });
    }

    [HttpGet]
    public async Task<IActionResult> Details(int id, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(id, cancellationToken);
        if (note is null || !await CanViewNoteAsync(note, cancellationToken))
        {
            return NotFound();
        }

        return View(await BuildNoteDetailsViewModelAsync(note, sharedView: false, cancellationToken));
    }

    [Authorize]
    [HttpGet]
    public IActionResult Create()
    {
        return View(new NoteEditViewModel());
    }

    [Authorize]
    [HttpPost]
    public async Task<IActionResult> Create(NoteEditViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var userId = _userManager.GetUserId(User)!;
        var note = new Note
        {
            Title = model.Title.Trim(),
            Content = model.Content.Trim(),
            IsPublic = model.IsPublic,
            OwnerId = userId,
            CreatedAtUtc = DateTime.UtcNow,
            UpdatedAtUtc = DateTime.UtcNow
        };

        _dbContext.Notes.Add(note);
        await PersistAttachmentsAsync(note, model.NewAttachments, cancellationToken);
        await _dbContext.SaveChangesAsync(cancellationToken);

        await _activityLogService.LogAsync("note.created", $"Note '{note.Title}' was created.", userId, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Your note was created.";
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [Authorize]
    [HttpGet]
    public async Task<IActionResult> Edit(int id, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(id, cancellationToken);
        if (note is null || !IsOwner(note))
        {
            return NotFound();
        }

        return View(new NoteEditViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            ExistingAttachments = note.Attachments
                .OrderBy(x => x.OriginalFileName)
                .Select(x => new AttachmentDisplayViewModel
                {
                    Id = x.Id,
                    OriginalFileName = x.OriginalFileName,
                    SizeBytes = x.SizeBytes
                })
                .ToList()
        });
    }

    [Authorize]
    [HttpPost]
    public async Task<IActionResult> Edit(NoteEditViewModel model, CancellationToken cancellationToken)
    {
        if (model.Id is null)
        {
            return NotFound();
        }

        var note = await LoadNoteAsync(model.Id.Value, cancellationToken);
        if (note is null || !IsOwner(note))
        {
            return NotFound();
        }

        if (!ModelState.IsValid)
        {
            model.ExistingAttachments = note.Attachments.Select(x => new AttachmentDisplayViewModel
            {
                Id = x.Id,
                OriginalFileName = x.OriginalFileName,
                SizeBytes = x.SizeBytes
            }).ToList();
            return View(model);
        }

        note.Title = model.Title.Trim();
        note.Content = model.Content.Trim();
        note.IsPublic = model.IsPublic;
        note.UpdatedAtUtc = DateTime.UtcNow;

        if (model.RemoveAttachmentIds.Count > 0)
        {
            var attachmentsToRemove = note.Attachments.Where(x => model.RemoveAttachmentIds.Contains(x.Id)).ToList();
            foreach (var attachment in attachmentsToRemove)
            {
                await _fileStorageService.DeleteAsync(attachment.StoredFileName, cancellationToken);
                _dbContext.Attachments.Remove(attachment);
            }
        }

        await PersistAttachmentsAsync(note, model.NewAttachments, cancellationToken);
        await _dbContext.SaveChangesAsync(cancellationToken);

        await _activityLogService.LogAsync("note.updated", $"Note '{note.Title}' was updated.", note.OwnerId, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Your note was updated.";
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [Authorize]
    [HttpGet]
    public async Task<IActionResult> Delete(int id, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(id, cancellationToken);
        if (note is null || !CanDelete(note))
        {
            return NotFound();
        }

        return View(await BuildNoteDetailsViewModelAsync(note, sharedView: false, cancellationToken));
    }

    [Authorize]
    [HttpPost]
    [ActionName("Delete")]
    public async Task<IActionResult> DeleteConfirmed(int id, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(id, cancellationToken);
        if (note is null || !CanDelete(note))
        {
            return NotFound();
        }

        foreach (var attachment in note.Attachments)
        {
            await _fileStorageService.DeleteAsync(attachment.StoredFileName, cancellationToken);
        }

        _dbContext.Notes.Remove(note);
        await _dbContext.SaveChangesAsync(cancellationToken);

        await _activityLogService.LogAsync("note.deleted", $"Note '{note.Title}' was deleted.", _userManager.GetUserId(User), HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "The note and its related data were deleted permanently.";
        return RedirectToAction(nameof(Index));
    }

    [Authorize]
    [HttpPost]
    public async Task<IActionResult> Rate(RateNoteInputModel model, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(model.NoteId, cancellationToken);
        if (note is null || !await CanViewNoteAsync(note, cancellationToken))
        {
            return NotFound();
        }

        if (!ModelState.IsValid)
        {
            TempData["StatusMessage"] = "Ratings must be between 1 and 5 stars.";
            return RedirectToAction(nameof(Details), new { id = model.NoteId });
        }

        var userId = _userManager.GetUserId(User)!;
        var rating = await _dbContext.Ratings.SingleOrDefaultAsync(x => x.NoteId == note.Id && x.UserId == userId, cancellationToken);
        if (rating is null)
        {
            rating = new Rating
            {
                NoteId = note.Id,
                UserId = userId,
                Value = model.Value,
                Comment = model.Comment?.Trim(),
                CreatedAtUtc = DateTime.UtcNow,
                UpdatedAtUtc = DateTime.UtcNow
            };
            _dbContext.Ratings.Add(rating);
        }
        else
        {
            rating.Value = model.Value;
            rating.Comment = model.Comment?.Trim();
            rating.UpdatedAtUtc = DateTime.UtcNow;
        }

        await _dbContext.SaveChangesAsync(cancellationToken);
        await _activityLogService.LogAsync("note.rated", $"A rating was recorded for note '{note.Title}'.", userId, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Your rating was saved.";
        return RedirectToAction(nameof(Details), new { id = note.Id, anchor = "ratings" });
    }

    [Authorize]
    [HttpGet]
    public async Task<IActionResult> Ratings(int id, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(id, cancellationToken);
        if (note is null || !(IsOwner(note) || User.IsInRole("Admin")))
        {
            return NotFound();
        }

        var model = new RatingListViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(x => x.Value) : 0,
            RatingCount = note.Ratings.Count,
            Ratings = note.Ratings
                .OrderByDescending(x => x.CreatedAtUtc)
                .Select(x => new RatingDisplayViewModel
                {
                    UserName = x.User.UserName ?? "Unknown",
                    Value = x.Value,
                    Comment = x.Comment,
                    CreatedAtUtc = x.UpdatedAtUtc
                })
                .ToList()
        };

        return View(model);
    }

    [Authorize]
    [HttpGet]
    public async Task<IActionResult> ManageShareLinks(int id, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(id, cancellationToken);
        if (note is null || !IsOwner(note))
        {
            return NotFound();
        }

        return View(BuildManageShareLinksViewModel(note));
    }

    [Authorize]
    [HttpPost]
    public async Task<IActionResult> RegenerateShareLink(int noteId, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(noteId, cancellationToken);
        if (note is null || !IsOwner(note))
        {
            return NotFound();
        }

        foreach (var link in note.ShareLinks.Where(x => x.RevokedAtUtc is null))
        {
            link.RevokedAtUtc = DateTime.UtcNow;
        }

        var payload = _shareLinkService.CreateTokenPayload();
        note.ShareLinks.Add(new ShareLink
        {
            TokenHash = payload.TokenHash,
            ProtectedToken = payload.ProtectedToken,
            CreatedAtUtc = DateTime.UtcNow
        });
        note.UpdatedAtUtc = DateTime.UtcNow;

        await _dbContext.SaveChangesAsync(cancellationToken);
        await _activityLogService.LogAsync("note.share_regenerated", $"Share links were regenerated for note '{note.Title}'.", note.OwnerId, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "A new share link was generated and prior links were revoked.";
        return RedirectToAction(nameof(ManageShareLinks), new { id = noteId });
    }

    [Authorize]
    [HttpPost]
    public async Task<IActionResult> RevokeShareLink(int noteId, int shareLinkId, CancellationToken cancellationToken)
    {
        var note = await LoadNoteAsync(noteId, cancellationToken);
        if (note is null || !IsOwner(note))
        {
            return NotFound();
        }

        var link = note.ShareLinks.SingleOrDefault(x => x.Id == shareLinkId && x.RevokedAtUtc is null);
        if (link is null)
        {
            return NotFound();
        }

        link.RevokedAtUtc = DateTime.UtcNow;
        note.UpdatedAtUtc = DateTime.UtcNow;
        await _dbContext.SaveChangesAsync(cancellationToken);

        await _activityLogService.LogAsync("note.share_revoked", $"A share link was revoked for note '{note.Title}'.", note.OwnerId, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "The share link was revoked.";
        return RedirectToAction(nameof(ManageShareLinks), new { id = noteId });
    }

    [HttpGet]
    public async Task<IActionResult> TopRated(CancellationToken cancellationToken)
    {
        var notes = await _dbContext.Notes
            .AsNoTracking()
            .Where(x => x.IsPublic && x.Ratings.Count >= 3)
            .OrderByDescending(x => x.Ratings.Average(r => r.Value))
            .ThenByDescending(x => x.Ratings.Count)
            .Select(x => new NoteCardViewModel
            {
                Id = x.Id,
                Title = x.Title,
                Excerpt = x.Content.Length > 200 ? x.Content.Substring(0, 200) + "..." : x.Content,
                OwnerUserName = x.Owner.UserName ?? "Unknown",
                CreatedAtUtc = x.CreatedAtUtc,
                UpdatedAtUtc = x.UpdatedAtUtc,
                IsPublic = x.IsPublic,
                AverageRating = x.Ratings.Average(r => r.Value),
                RatingCount = x.Ratings.Count
            })
            .ToListAsync(cancellationToken);

        return View(new TopRatedViewModel { Notes = notes });
    }

    [HttpGet]
    public async Task<IActionResult> DownloadAttachment(int id, CancellationToken cancellationToken)
    {
        var attachment = await _dbContext.Attachments
            .Include(x => x.Note)
            .SingleOrDefaultAsync(x => x.Id == id, cancellationToken);

        if (attachment is null || !await CanViewNoteAsync(attachment.Note, cancellationToken))
        {
            return NotFound();
        }

        var path = _fileStorageService.GetAbsolutePath(attachment.StoredFileName);
        if (!System.IO.File.Exists(path))
        {
            return NotFound();
        }

        return PhysicalFile(path, attachment.ContentType, attachment.OriginalFileName);
    }

    private async Task<Note?> LoadNoteAsync(int id, CancellationToken cancellationToken)
    {
        return await _dbContext.Notes
            .Include(x => x.Owner)
            .Include(x => x.Attachments)
            .Include(x => x.Ratings)
                .ThenInclude(x => x.User)
            .Include(x => x.ShareLinks)
            .SingleOrDefaultAsync(x => x.Id == id, cancellationToken);
    }

    private async Task<bool> CanViewNoteAsync(Note note, CancellationToken cancellationToken)
    {
        if (note.IsPublic)
        {
            return true;
        }

        if (User.Identity?.IsAuthenticated != true)
        {
            return false;
        }

        return IsOwner(note) || User.IsInRole("Admin");
    }

    private bool IsOwner(Note note)
    {
        return string.Equals(note.OwnerId, _userManager.GetUserId(User), StringComparison.Ordinal);
    }

    private bool CanDelete(Note note)
    {
        return IsOwner(note) || User.IsInRole("Admin");
    }

    private async Task PersistAttachmentsAsync(Note note, IEnumerable<IFormFile> files, CancellationToken cancellationToken)
    {
        foreach (var file in files.Where(x => x.Length > 0))
        {
            var stored = await _fileStorageService.SaveAsync(file, cancellationToken);
            note.Attachments.Add(new Attachment
            {
                StoredFileName = stored.StoredFileName,
                OriginalFileName = stored.OriginalFileName,
                ContentType = stored.ContentType,
                SizeBytes = stored.SizeBytes,
                UploadedAtUtc = DateTime.UtcNow
            });
        }
    }

    private async Task<NoteDetailsViewModel> BuildNoteDetailsViewModelAsync(Note note, bool sharedView, CancellationToken cancellationToken)
    {
        var currentUserId = User.Identity?.IsAuthenticated == true ? _userManager.GetUserId(User) : null;
        var existingRating = currentUserId is null
            ? null
            : note.Ratings.SingleOrDefault(x => x.UserId == currentUserId);

        return new NoteDetailsViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            OwnerUserName = note.Owner.UserName ?? "Unknown",
            CreatedAtUtc = note.CreatedAtUtc,
            UpdatedAtUtc = note.UpdatedAtUtc,
            Attachments = note.Attachments
                .OrderBy(x => x.OriginalFileName)
                .Select(x => new AttachmentDisplayViewModel
                {
                    Id = x.Id,
                    OriginalFileName = x.OriginalFileName,
                    SizeBytes = x.SizeBytes
                })
                .ToList(),
            Ratings = note.Ratings
                .OrderByDescending(x => x.UpdatedAtUtc)
                .Select(x => new RatingDisplayViewModel
                {
                    UserName = x.User.UserName ?? "Unknown",
                    Value = x.Value,
                    Comment = x.Comment,
                    CreatedAtUtc = x.UpdatedAtUtc,
                    IsCurrentUsersRating = x.UserId == currentUserId
                })
                .ToList(),
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(x => x.Value) : 0,
            RatingCount = note.Ratings.Count,
            CanEdit = !sharedView && IsOwner(note),
            CanDelete = !sharedView && CanDelete(note),
            CanManageShares = !sharedView && IsOwner(note),
            CanViewRatings = !sharedView && (IsOwner(note) || User.IsInRole("Admin")),
            CanRate = !sharedView && User.Identity?.IsAuthenticated == true,
            IsSharedView = sharedView,
            RatingForm = new RateNoteInputModel
            {
                NoteId = note.Id,
                Value = existingRating?.Value ?? 5,
                Comment = existingRating?.Comment
            }
        };
    }

    private ManageShareLinksViewModel BuildManageShareLinksViewModel(Note note)
    {
        return new ManageShareLinksViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            ActiveLinks = note.ShareLinks
                .Where(x => x.RevokedAtUtc is null)
                .OrderByDescending(x => x.CreatedAtUtc)
                .Select(x => new ShareLinkDisplayViewModel
                {
                    Id = x.Id,
                    CreatedAtUtc = x.CreatedAtUtc,
                    Url = Url.Action("Open", "Share", new { token = _shareLinkService.RevealToken(x.ProtectedToken) }, Request.Scheme) ?? string.Empty
                })
                .ToList()
        };
    }
}
