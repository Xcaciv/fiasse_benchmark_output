using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using rawdog.Data;
using rawdog.Models;
using rawdog.Services;
using rawdog.ViewModels;

namespace rawdog.Controllers;

public sealed class NotesController(
    ApplicationDbContext dbContext,
    IFileStorageService fileStorageService,
    IActivityLogger activityLogger) : Controller
{
    [Authorize]
    public async Task<IActionResult> Index(CancellationToken cancellationToken)
    {
        var currentUserId = GetCurrentUserId();

        var notes = await dbContext.Notes
            .Where(note => note.OwnerId == currentUserId)
            .OrderByDescending(note => note.UpdatedAtUtc ?? note.CreatedAtUtc)
            .Select(note => new NoteListItemViewModel
            {
                Id = note.Id,
                Title = note.Title,
                IsPublic = note.IsPublic,
                CreatedAtUtc = note.CreatedAtUtc,
                UpdatedAtUtc = note.UpdatedAtUtc,
                AttachmentCount = note.Attachments.Count,
                RatingCount = note.Ratings.Count,
                AverageRating = note.Ratings.Any() ? note.Ratings.Average(rating => (double)rating.Score) : 0
            })
            .ToListAsync(cancellationToken);

        return View(new NoteIndexViewModel { Notes = notes });
    }

    [Authorize]
    public IActionResult Create()
    {
        return View(new NoteFormViewModel());
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(NoteFormViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var note = new Note
        {
            Title = model.Title.Trim(),
            Content = model.Content.Trim(),
            IsPublic = model.IsPublic,
            CreatedAtUtc = DateTime.UtcNow,
            OwnerId = GetCurrentUserId()
        };

        dbContext.Notes.Add(note);
        await dbContext.SaveChangesAsync(cancellationToken);

        if (model.UploadedFiles.Count > 0)
        {
            var storedFiles = await fileStorageService.SaveFilesAsync(model.UploadedFiles, cancellationToken);
            foreach (var storedFile in storedFiles)
            {
                note.Attachments.Add(new NoteAttachment
                {
                    OriginalFileName = storedFile.OriginalFileName,
                    StoredFileName = storedFile.StoredFileName,
                    ContentType = storedFile.ContentType,
                    SizeBytes = storedFile.SizeBytes,
                    UploadedAtUtc = DateTime.UtcNow
                });
            }

            await dbContext.SaveChangesAsync(cancellationToken);
        }

        await activityLogger.LogAsync("notes.create", $"Created note '{note.Title}'.", note.OwnerId, cancellationToken);
        TempData["StatusMessage"] = "Note created successfully.";
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [AllowAnonymous]
    public async Task<IActionResult> Details(int id, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.Owner)
            .Include(item => item.Attachments)
            .Include(item => item.Ratings)
                .ThenInclude(rating => rating.User)
            .Include(item => item.ShareLinks)
            .SingleOrDefaultAsync(item => item.Id == id, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanViewNote(note))
        {
            return User.Identity?.IsAuthenticated == true ? Forbid() : Challenge();
        }

        return View(ToDetailsViewModel(note, attachmentToken: null, accessedByShareLink: false));
    }

    [Authorize]
    public async Task<IActionResult> Edit(int id, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.Attachments)
            .SingleOrDefaultAsync(item => item.Id == id, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(note))
        {
            return Forbid();
        }

        var model = new NoteFormViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            ExistingAttachments = note.Attachments
                .OrderByDescending(attachment => attachment.UploadedAtUtc)
                .Select(attachment => new NoteAttachmentItemViewModel
                {
                    Id = attachment.Id,
                    OriginalFileName = attachment.OriginalFileName,
                    ContentType = attachment.ContentType,
                    SizeBytes = attachment.SizeBytes
                })
                .ToList()
        };

        return View(model);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, NoteFormViewModel model, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.Attachments)
            .SingleOrDefaultAsync(item => item.Id == id, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(note))
        {
            return Forbid();
        }

        if (!ModelState.IsValid)
        {
            model.ExistingAttachments = note.Attachments
                .Select(attachment => new NoteAttachmentItemViewModel
                {
                    Id = attachment.Id,
                    OriginalFileName = attachment.OriginalFileName,
                    ContentType = attachment.ContentType,
                    SizeBytes = attachment.SizeBytes
                })
                .ToList();

            return View(model);
        }

        note.Title = model.Title.Trim();
        note.Content = model.Content.Trim();
        note.IsPublic = model.IsPublic;
        note.UpdatedAtUtc = DateTime.UtcNow;

        if (model.UploadedFiles.Count > 0)
        {
            var storedFiles = await fileStorageService.SaveFilesAsync(model.UploadedFiles, cancellationToken);
            foreach (var storedFile in storedFiles)
            {
                note.Attachments.Add(new NoteAttachment
                {
                    OriginalFileName = storedFile.OriginalFileName,
                    StoredFileName = storedFile.StoredFileName,
                    ContentType = storedFile.ContentType,
                    SizeBytes = storedFile.SizeBytes,
                    UploadedAtUtc = DateTime.UtcNow
                });
            }
        }

        await dbContext.SaveChangesAsync(cancellationToken);
        await activityLogger.LogAsync("notes.edit", $"Updated note '{note.Title}'.", note.OwnerId, cancellationToken);

        TempData["StatusMessage"] = "Note updated successfully.";
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [Authorize]
    public async Task<IActionResult> Delete(int id, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.Owner)
            .SingleOrDefaultAsync(item => item.Id == id, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(note))
        {
            return Forbid();
        }

        return View(note);
    }

    [HttpPost, ActionName(nameof(Delete))]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.Attachments)
            .SingleOrDefaultAsync(item => item.Id == id, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(note))
        {
            return Forbid();
        }

        foreach (var attachment in note.Attachments)
        {
            await fileStorageService.DeleteFileAsync(attachment.StoredFileName, cancellationToken);
        }

        dbContext.Notes.Remove(note);
        await dbContext.SaveChangesAsync(cancellationToken);

        await activityLogger.LogAsync("notes.delete", $"Deleted note '{note.Title}'.", GetCurrentUserId(), cancellationToken);
        TempData["StatusMessage"] = "Note deleted permanently.";
        return RedirectToAction(nameof(Index));
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int noteId, int attachmentId, CancellationToken cancellationToken)
    {
        var attachment = await dbContext.Attachments
            .Include(item => item.Note)
            .SingleOrDefaultAsync(item => item.Id == attachmentId && item.NoteId == noteId, cancellationToken);

        if (attachment is null || attachment.Note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(attachment.Note))
        {
            return Forbid();
        }

        await fileStorageService.DeleteFileAsync(attachment.StoredFileName, cancellationToken);
        dbContext.Attachments.Remove(attachment);
        attachment.Note.UpdatedAtUtc = DateTime.UtcNow;
        await dbContext.SaveChangesAsync(cancellationToken);

        TempData["StatusMessage"] = "Attachment removed.";
        return RedirectToAction(nameof(Edit), new { id = noteId });
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int noteId, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.ShareLinks)
            .SingleOrDefaultAsync(item => item.Id == noteId, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(note))
        {
            return Forbid();
        }

        foreach (var link in note.ShareLinks.Where(link => link.RevokedAtUtc is null))
        {
            link.RevokedAtUtc = DateTime.UtcNow;
        }

        note.ShareLinks.Add(new NoteShareLink
        {
            Token = Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant(),
            CreatedAtUtc = DateTime.UtcNow
        });
        note.UpdatedAtUtc = DateTime.UtcNow;

        await dbContext.SaveChangesAsync(cancellationToken);
        await activityLogger.LogAsync("notes.share_generated", $"Generated a share link for note '{note.Title}'.", GetCurrentUserId(), cancellationToken);

        TempData["StatusMessage"] = "A new share link has been generated.";
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLinks(int noteId, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes
            .Include(item => item.ShareLinks)
            .SingleOrDefaultAsync(item => item.Id == noteId, cancellationToken);

        if (note is null)
        {
            return NotFound();
        }

        if (!CanManageNote(note))
        {
            return Forbid();
        }

        foreach (var link in note.ShareLinks.Where(link => link.RevokedAtUtc is null))
        {
            link.RevokedAtUtc = DateTime.UtcNow;
        }

        note.UpdatedAtUtc = DateTime.UtcNow;
        await dbContext.SaveChangesAsync(cancellationToken);

        await activityLogger.LogAsync("notes.share_revoked", $"Revoked share links for note '{note.Title}'.", GetCurrentUserId(), cancellationToken);
        TempData["StatusMessage"] = "All active share links have been revoked.";
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Rate(int noteId, NoteRatingInputViewModel input, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            TempData["ErrorMessage"] = "Please submit a rating between 1 and 5 stars.";
            return RedirectToAction(nameof(Details), new { id = noteId });
        }

        var note = await dbContext.Notes.SingleOrDefaultAsync(item => item.Id == noteId, cancellationToken);
        if (note is null)
        {
            return NotFound();
        }

        if (!CanViewNote(note))
        {
            return Forbid();
        }

        var currentUserId = GetCurrentUserId();
        var existingRating = await dbContext.Ratings.SingleOrDefaultAsync(
            rating => rating.NoteId == noteId && rating.UserId == currentUserId,
            cancellationToken);

        if (existingRating is null)
        {
            dbContext.Ratings.Add(new NoteRating
            {
                NoteId = noteId,
                UserId = currentUserId,
                Score = input.Score,
                Comment = string.IsNullOrWhiteSpace(input.Comment) ? null : input.Comment.Trim(),
                CreatedAtUtc = DateTime.UtcNow
            });
        }
        else
        {
            existingRating.Score = input.Score;
            existingRating.Comment = string.IsNullOrWhiteSpace(input.Comment) ? null : input.Comment.Trim();
            existingRating.UpdatedAtUtc = DateTime.UtcNow;
        }

        await dbContext.SaveChangesAsync(cancellationToken);
        await activityLogger.LogAsync("notes.rate", $"Rated note '{note.Title}'.", currentUserId, cancellationToken);

        TempData["StatusMessage"] = existingRating is null ? "Your rating has been added." : "Your rating has been updated.";
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [AllowAnonymous]
    public async Task<IActionResult> DownloadAttachment(int id, string? token, CancellationToken cancellationToken)
    {
        var attachment = await dbContext.Attachments
            .Include(item => item.Note)
            .SingleOrDefaultAsync(item => item.Id == id, cancellationToken);

        if (attachment is null || attachment.Note is null)
        {
            return NotFound();
        }

        var hasShareAccess = !string.IsNullOrWhiteSpace(token) && await dbContext.ShareLinks.AnyAsync(
            link => link.Token == token && link.NoteId == attachment.NoteId && link.RevokedAtUtc == null,
            cancellationToken);

        if (!(CanViewNote(attachment.Note) || hasShareAccess))
        {
            return User.Identity?.IsAuthenticated == true ? Forbid() : Challenge();
        }

        return PhysicalFile(fileStorageService.GetAbsolutePath(attachment.StoredFileName), attachment.ContentType, attachment.OriginalFileName);
    }

    private NoteDetailsViewModel ToDetailsViewModel(Note note, string? attachmentToken, bool accessedByShareLink)
    {
        var currentUserId = User.Identity?.IsAuthenticated == true ? GetCurrentUserId() : null;
        var isManager = CanManageNote(note);
        var activeShareLink = note.ShareLinks.FirstOrDefault(link => link.RevokedAtUtc is null);
        var currentRating = note.Ratings.FirstOrDefault(rating => rating.UserId == currentUserId);

        return new NoteDetailsViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            Author = note.Owner?.UserName ?? "Unknown",
            IsPublic = note.IsPublic,
            CreatedAtUtc = note.CreatedAtUtc,
            UpdatedAtUtc = note.UpdatedAtUtc,
            AverageRating = note.Ratings.Count == 0 ? 0 : note.Ratings.Average(rating => rating.Score),
            RatingCount = note.Ratings.Count,
            Attachments = note.Attachments
                .OrderByDescending(attachment => attachment.UploadedAtUtc)
                .Select(attachment => new NoteAttachmentItemViewModel
                {
                    Id = attachment.Id,
                    OriginalFileName = attachment.OriginalFileName,
                    ContentType = attachment.ContentType,
                    SizeBytes = attachment.SizeBytes
                })
                .ToList(),
            Ratings = note.Ratings
                .OrderByDescending(rating => rating.UpdatedAtUtc ?? rating.CreatedAtUtc)
                .Select(rating => new NoteRatingItemViewModel
                {
                    Score = rating.Score,
                    Comment = rating.Comment,
                    UserName = rating.User?.UserName ?? "Unknown",
                    CreatedAtUtc = rating.UpdatedAtUtc ?? rating.CreatedAtUtc
                })
                .ToList(),
            CanEdit = isManager,
            CanDelete = isManager,
            CanManageShareLinks = isManager,
            CanRate = User.Identity?.IsAuthenticated == true && !accessedByShareLink,
            ActiveShareUrl = activeShareLink is null
                ? null
                : Url.Action("Details", "Shared", new { token = activeShareLink.Token }, Request.Scheme),
            RatingInput = new NoteRatingInputViewModel
            {
                Score = currentRating?.Score ?? 5,
                Comment = currentRating?.Comment
            },
            AccessedByShareLink = accessedByShareLink,
            AttachmentToken = attachmentToken
        };
    }

    private bool CanViewNote(Note note)
    {
        if (note.IsPublic)
        {
            return true;
        }

        if (User.Identity?.IsAuthenticated != true)
        {
            return false;
        }

        return note.OwnerId == GetCurrentUserId() || User.IsInRole("Admin");
    }

    private bool CanManageNote(Note note)
    {
        return User.Identity?.IsAuthenticated == true &&
               (note.OwnerId == GetCurrentUserId() || User.IsInRole("Admin"));
    }

    private string GetCurrentUserId()
    {
        return User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? throw new InvalidOperationException("The current user identifier is not available.");
    }
}
