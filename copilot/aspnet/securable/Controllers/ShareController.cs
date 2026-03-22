using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;

namespace LooseNotes.Controllers;

public sealed class ShareController : Controller
{
    private readonly ApplicationDbContext _dbContext;
    private readonly IShareLinkService _shareLinkService;
    private readonly IFileStorageService _fileStorageService;

    public ShareController(ApplicationDbContext dbContext, IShareLinkService shareLinkService, IFileStorageService fileStorageService)
    {
        _dbContext = dbContext;
        _shareLinkService = shareLinkService;
        _fileStorageService = fileStorageService;
    }

    [HttpGet("share/{token}")]
    public async Task<IActionResult> Open(string token, CancellationToken cancellationToken)
    {
        var shareLink = await LoadShareLinkAsync(token, cancellationToken);
        if (shareLink is null)
        {
            return NotFound();
        }

        var note = shareLink.Note;
        var model = new NoteDetailsViewModel
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
                    CreatedAtUtc = x.UpdatedAtUtc
                })
                .ToList(),
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(x => x.Value) : 0,
            RatingCount = note.Ratings.Count,
            IsSharedView = true
        };

        ViewBag.ShareToken = token;
        return View(model);
    }

    [HttpGet("share/{token}/attachments/{attachmentId:int}")]
    public async Task<IActionResult> DownloadAttachment(string token, int attachmentId, CancellationToken cancellationToken)
    {
        var shareLink = await LoadShareLinkAsync(token, cancellationToken);
        if (shareLink is null)
        {
            return NotFound();
        }

        var attachment = shareLink.Note.Attachments.SingleOrDefault(x => x.Id == attachmentId);
        if (attachment is null)
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

    private async Task<Models.ShareLink?> LoadShareLinkAsync(string token, CancellationToken cancellationToken)
    {
        var tokenHash = _shareLinkService.HashToken(token);
        return await _dbContext.ShareLinks
            .AsNoTracking()
            .Include(x => x.Note)
                .ThenInclude(x => x.Owner)
            .Include(x => x.Note)
                .ThenInclude(x => x.Attachments)
            .Include(x => x.Note)
                .ThenInclude(x => x.Ratings)
                    .ThenInclude(x => x.User)
            .SingleOrDefaultAsync(x => x.TokenHash == tokenHash && x.RevokedAtUtc == null, cancellationToken);
    }
}
