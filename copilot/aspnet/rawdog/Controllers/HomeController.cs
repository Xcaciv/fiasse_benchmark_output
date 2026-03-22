using System.Diagnostics;
using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using rawdog.Data;
using rawdog.Models;
using rawdog.ViewModels;

namespace rawdog.Controllers;

public sealed class HomeController(ApplicationDbContext dbContext) : Controller
{
    public async Task<IActionResult> Index(CancellationToken cancellationToken)
    {
        var model = new HomeIndexViewModel
        {
            UserCount = await dbContext.Users.CountAsync(cancellationToken),
            PublicNoteCount = await dbContext.Notes.CountAsync(note => note.IsPublic, cancellationToken),
            RatingCount = await dbContext.Ratings.CountAsync(cancellationToken),
            LatestPublicNotes = await dbContext.Notes
                .Where(note => note.IsPublic)
                .OrderByDescending(note => note.CreatedAtUtc)
                .Take(6)
                .Select(note => new SearchResultItemViewModel
                {
                    Id = note.Id,
                    Title = note.Title,
                    Author = note.Owner!.UserName ?? "Unknown",
                    CreatedAtUtc = note.CreatedAtUtc,
                    Excerpt = note.Content.Length > 200 ? note.Content.Substring(0, 200) + "..." : note.Content,
                    IsPublic = note.IsPublic
                })
                .ToListAsync(cancellationToken),
            TopRatedNotes = await dbContext.Notes
                .Where(note => note.IsPublic && note.Ratings.Count >= 3)
                .OrderByDescending(note => note.Ratings.Average(rating => (double)rating.Score))
                .ThenByDescending(note => note.Ratings.Count)
                .Take(5)
                .Select(note => new TopRatedNoteViewModel
                {
                    Id = note.Id,
                    Title = note.Title,
                    Author = note.Owner!.UserName ?? "Unknown",
                    Preview = note.Content.Length > 200 ? note.Content.Substring(0, 200) + "..." : note.Content,
                    AverageRating = note.Ratings.Average(rating => (double)rating.Score),
                    RatingCount = note.Ratings.Count
                })
                .ToListAsync(cancellationToken)
        };

        return View(model);
    }

    public async Task<IActionResult> Search(string q, CancellationToken cancellationToken)
    {
        var query = (q ?? string.Empty).Trim();
        var currentUserId = User.Identity?.IsAuthenticated == true ? User.FindFirstValue(System.Security.Claims.ClaimTypes.NameIdentifier) : null;
        var pattern = $"%{query}%";

        var results = query.Length == 0
            ? new List<SearchResultItemViewModel>()
            : await dbContext.Notes
                .Where(note => note.IsPublic || note.OwnerId == currentUserId)
                .Where(note => EF.Functions.Like(note.Title, pattern) || EF.Functions.Like(note.Content, pattern))
                .OrderByDescending(note => note.CreatedAtUtc)
                .Select(note => new SearchResultItemViewModel
                {
                    Id = note.Id,
                    Title = note.Title,
                    Author = note.Owner!.UserName ?? "Unknown",
                    CreatedAtUtc = note.CreatedAtUtc,
                    Excerpt = note.Content.Length > 200 ? note.Content.Substring(0, 200) + "..." : note.Content,
                    IsPublic = note.IsPublic
                })
                .ToListAsync(cancellationToken);

        return View(new SearchViewModel
        {
            Query = query,
            Results = results
        });
    }

    public async Task<IActionResult> TopRated(CancellationToken cancellationToken)
    {
        var notes = await dbContext.Notes
            .Where(note => note.IsPublic && note.Ratings.Count >= 3)
            .OrderByDescending(note => note.Ratings.Average(rating => (double)rating.Score))
            .ThenByDescending(note => note.Ratings.Count)
            .Select(note => new TopRatedNoteViewModel
            {
                Id = note.Id,
                Title = note.Title,
                Author = note.Owner!.UserName ?? "Unknown",
                Preview = note.Content.Length > 200 ? note.Content.Substring(0, 200) + "..." : note.Content,
                AverageRating = note.Ratings.Average(rating => (double)rating.Score),
                RatingCount = note.Ratings.Count
            })
            .ToListAsync(cancellationToken);

        return View(notes);
    }

    public IActionResult Privacy()
    {
        return View();
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
    }
}
