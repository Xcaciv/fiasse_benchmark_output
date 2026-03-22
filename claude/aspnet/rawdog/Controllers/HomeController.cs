using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

public class HomeController : Controller
{
    private readonly ApplicationDbContext _db;

    public HomeController(ApplicationDbContext db)
    {
        _db = db;
    }

    public async Task<IActionResult> Index()
    {
        var topRated = await _db.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .Select(n => new TopRatedNoteViewModel
            {
                Id = n.Id,
                Title = n.Title,
                AuthorName = n.User!.UserName ?? "Unknown",
                AverageRating = n.Ratings.Average(r => (double)r.Value),
                RatingCount = n.Ratings.Count,
                Preview = n.Content.Length > 200 ? n.Content.Substring(0, 200) : n.Content
            })
            .OrderByDescending(n => n.AverageRating)
            .Take(5)
            .ToListAsync();

        return View(topRated);
    }
}
