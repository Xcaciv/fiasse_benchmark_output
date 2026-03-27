using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;

namespace LooseNotes.Controllers;

public class HomeController : Controller
{
    private readonly ILogger<HomeController> _logger;
    private readonly ApplicationDbContext _context;

    public HomeController(ILogger<HomeController> logger, ApplicationDbContext context)
    {
        _logger = logger;
        _context = context;
    }

    public async Task<IActionResult> Index()
    {
        var recentNotes = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic)
            .OrderByDescending(n => n.CreatedAt)
            .Take(10)
            .ToListAsync();

        return View(recentNotes);
    }

    public IActionResult Privacy()
    {
        return View();
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View();
    }
}
