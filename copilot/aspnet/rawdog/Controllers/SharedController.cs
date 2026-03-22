using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using rawdog.Data;
using rawdog.ViewModels;

namespace rawdog.Controllers;

public sealed class SharedController(ApplicationDbContext dbContext) : Controller
{
    [HttpGet("/s/{token}")]
    public async Task<IActionResult> Details(string token, CancellationToken cancellationToken)
    {
        var shareLink = await dbContext.ShareLinks
            .Include(link => link.Note)!
                .ThenInclude(note => note!.Owner)
            .Include(link => link.Note)!
                .ThenInclude(note => note!.Attachments)
            .Include(link => link.Note)!
                .ThenInclude(note => note!.Ratings)
                    .ThenInclude(rating => rating.User)
            .Include(link => link.Note)!
                .ThenInclude(note => note!.ShareLinks)
            .SingleOrDefaultAsync(link => link.Token == token && link.RevokedAtUtc == null, cancellationToken);

        if (shareLink?.Note is null)
        {
            return NotFound();
        }

        var note = shareLink.Note;
        var model = new NoteDetailsViewModel
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
            CanEdit = false,
            CanDelete = false,
            CanManageShareLinks = false,
            CanRate = false,
            ActiveShareUrl = Url.Action(nameof(Details), "Shared", new { token = shareLink.Token }, Request.Scheme),
            AccessedByShareLink = true,
            AttachmentToken = shareLink.Token
        };

        return View("~/Views/Notes/Details.cshtml", model);
    }
}
