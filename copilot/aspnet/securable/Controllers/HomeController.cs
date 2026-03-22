using System.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels.Home;
using LooseNotes.ViewModels.Notes;

namespace LooseNotes.Controllers;

public sealed class HomeController : Controller
{
    private readonly ApplicationDbContext _dbContext;

    public HomeController(ApplicationDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken cancellationToken)
    {
        var model = new HomeIndexViewModel
        {
            PublicNoteCount = await _dbContext.Notes.CountAsync(x => x.IsPublic, cancellationToken),
            UserCount = await _dbContext.Users.CountAsync(cancellationToken),
            RecentPublicNotes = await _dbContext.Notes
                .AsNoTracking()
                .Where(x => x.IsPublic)
                .OrderByDescending(x => x.CreatedAtUtc)
                .Take(5)
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
                .ToListAsync(cancellationToken),
            TopRatedNotes = await _dbContext.Notes
                .AsNoTracking()
                .Where(x => x.IsPublic && x.Ratings.Count >= 3)
                .OrderByDescending(x => x.Ratings.Average(r => r.Value))
                .ThenByDescending(x => x.Ratings.Count)
                .Take(5)
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
                .ToListAsync(cancellationToken)
        };

        return View(model);
    }

    [HttpGet]
    public IActionResult AccessDenied()
    {
        return View();
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    [HttpGet]
    public IActionResult Error()
    {
        return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
    }
}
