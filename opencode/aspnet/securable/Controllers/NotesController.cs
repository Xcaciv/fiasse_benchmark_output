using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Models;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

[Authorize]
public class NotesController : Controller
{
    private readonly INoteService _noteService;
    private readonly IFileService _fileService;
    private readonly ILogger<NotesController> _logger;

    public NotesController(
        INoteService noteService,
        IFileService fileService,
        ILogger<NotesController> logger)
    {
        _noteService = noteService;
        _fileService = fileService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var notes = await _noteService.GetUserNotesAsync(userId);
        return View(notes);
    }

    [HttpGet]
    public IActionResult Create()
    {
        return View(new NoteViewModel());
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(NoteViewModel model)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            UserId = userId
        };

        var createdNote = await _noteService.CreateNoteAsync(note);

        if (model.Files != null && model.Files.Any())
        {
            foreach (var file in model.Files.Where(f => f.Length > 0))
            {
                if (_fileService.IsValidFileType(file.FileName) && 
                    _fileService.IsValidFileSize(file.Length))
                {
                    await _fileService.SaveFileAsync(file, createdNote.Id);
                }
                else
                {
                    ModelState.AddModelError(string.Empty, 
                        $"Invalid file type or size: {file.FileName}");
                }
            }
        }

        _logger.LogInformation("Note created: {NoteId} by user {UserId}", createdNote.Id, userId);
        
        return RedirectToAction("Details", new { id = createdNote.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Details(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var isAdmin = User.IsInRole("Admin");
        var canAccess = await _noteService.CanUserAccessNoteAsync(id, userId, isAdmin);
        
        if (!canAccess)
        {
            return Forbid();
        }

        var note = await _noteService.GetNoteByIdAsync(id);
        if (note == null)
        {
            return NotFound();
        }

        return View(note);
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var isAdmin = User.IsInRole("Admin");
        var canAccess = await _noteService.CanUserAccessNoteAsync(id, userId, isAdmin);
        
        if (!canAccess)
        {
            return Forbid();
        }

        var note = await _noteService.GetNoteByIdAsync(id);
        if (note == null)
        {
            return NotFound();
        }

        var model = new NoteViewModel
        {
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        };

        ViewData["NoteId"] = id;
        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, NoteViewModel model)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        if (!ModelState.IsValid)
        {
            ViewData["NoteId"] = id;
            return View(model);
        }

        var existingNote = await _noteService.GetNoteByIdAsync(id);
        if (existingNote == null)
        {
            return NotFound();
        }

        var isAdmin = User.IsInRole("Admin");
        if (existingNote.UserId != userId && !isAdmin)
        {
            return Forbid();
        }

        existingNote.Title = model.Title;
        existingNote.Content = model.Content;
        existingNote.IsPublic = model.IsPublic;

        await _noteService.UpdateNoteAsync(existingNote);

        if (model.Files != null && model.Files.Any())
        {
            foreach (var file in model.Files.Where(f => f.Length > 0))
            {
                if (_fileService.IsValidFileType(file.FileName) && 
                    _fileService.IsValidFileSize(file.Length))
                {
                    await _fileService.SaveFileAsync(file, id);
                }
            }
        }

        return RedirectToAction("Details", new { id });
    }

    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var note = await _noteService.GetNoteByIdAsync(id);
        if (note == null)
        {
            return NotFound();
        }

        var isAdmin = User.IsInRole("Admin");
        if (note.UserId != userId && !isAdmin)
        {
            return Forbid();
        }

        return View(note);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var isAdmin = User.IsInRole("Admin");
        var result = await _noteService.DeleteNoteAsync(id, userId, isAdmin);

        if (!result)
        {
            return NotFound();
        }

        return RedirectToAction("Index");
    }

    [HttpGet]
    public async Task<IActionResult> Share(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var note = await _noteService.GetNoteByIdAsync(id);
        if (note == null || note.UserId != userId)
        {
            return Forbid();
        }

        var shareLink = await _noteService.GetActiveShareLinkAsync(id);
        
        if (shareLink == null)
        {
            shareLink = await _noteService.CreateShareLinkAsync(id);
        }

        ViewData["ShareUrl"] = Url.Action("ViewShared", "Notes", 
            new { token = shareLink.Token }, protocol: Request.Scheme);
        
        return View(note);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RegenerateShareLink(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var note = await _noteService.GetNoteByIdAsync(id);
        if (note == null || note.UserId != userId)
        {
            return Forbid();
        }

        await _noteService.RevokeShareLinkAsync(id, userId);
        var newLink = await _noteService.CreateShareLinkAsync(id);

        return RedirectToAction("Share", new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        await _noteService.RevokeShareLinkAsync(id, userId);
        
        return RedirectToAction("Details", new { id });
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> ViewShared(string token)
    {
        var note = await _noteService.GetSharedNoteByTokenAsync(token);
        
        if (note == null)
        {
            return NotFound();
        }

        return View("Details", note);
    }

    [HttpGet]
    public async Task<IActionResult> Ratings(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        var note = await _noteService.GetNoteByIdAsync(id);
        if (note == null)
        {
            return NotFound();
        }

        var isAdmin = User.IsInRole("Admin");
        if (note.UserId != userId && !isAdmin)
        {
            return Forbid();
        }

        var ratings = await _noteService.GetNoteRatingsAsync(id);
        
        var model = new NoteRatingsViewModel
        {
            Note = note,
            Ratings = ratings
        };

        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> AddRating(int noteId, int value, string? comment)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        if (string.IsNullOrEmpty(userId))
        {
            return RedirectToAction("Login", "Account");
        }

        if (value < 1 || value > 5)
        {
            return BadRequest("Rating must be between 1 and 5");
        }

        await _noteService.AddOrUpdateRatingAsync(noteId, userId, value, comment);
        
        return RedirectToAction("Details", new { id = noteId });
    }
}

public class NoteViewModel
{
    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public List<IFormFile>? Files { get; set; }
}

public class NoteRatingsViewModel
{
    public Note Note { get; set; } = null!;
    public IEnumerable<Rating> Ratings { get; set; } = Enumerable.Empty<Rating>();
}
