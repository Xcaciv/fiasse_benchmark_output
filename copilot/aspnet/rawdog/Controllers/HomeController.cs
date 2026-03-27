using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Controllers;

public class HomeController : Controller
{
    private readonly ApplicationDbContext _context;

    public HomeController(ApplicationDbContext context)
    {
        _context = context;
    }

    public async Task<IActionResult> Index()
    {
        var recentPublic = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic)
            .OrderByDescending(n => n.CreatedAt)
            .Take(6)
            .ToListAsync();
        return View(recentPublic);
    }

    public async Task<IActionResult> Search(string q, int page = 1)
    {
        ViewBag.Query = q;
        if (string.IsNullOrWhiteSpace(q))
            return View(new List<Note>());

        var currentUserId = User.Identity?.IsAuthenticated == true
            ? _context.Users.FirstOrDefault(u => u.UserName == User.Identity.Name)?.Id
            : null;

        var query = _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n =>
                (n.IsPublic || (currentUserId != null && n.UserId == currentUserId)) &&
                (n.Title.ToLower().Contains(q.ToLower()) || n.Content.ToLower().Contains(q.ToLower())));

        var results = await query.OrderByDescending(n => n.CreatedAt).ToListAsync();
        return View(results);
    }

    public async Task<IActionResult> TopRated()
    {
        var topRated = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .ToListAsync();

        var ordered = topRated
            .OrderByDescending(n => n.Ratings.Average(r => r.Stars))
            .Take(20)
            .ToList();

        return View(ordered);
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View();
    }
}
