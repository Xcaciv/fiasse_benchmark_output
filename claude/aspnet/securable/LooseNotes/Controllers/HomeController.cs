using LooseNotes.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics;

namespace LooseNotes.Controllers;

public class HomeController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<HomeController> _logger;

    public HomeController(ApplicationDbContext db, ILogger<HomeController> logger)
    {
        _db = db;
        _logger = logger;
    }

    [HttpGet]
    public IActionResult Index() => View();

    /// <summary>
    /// Top-rated public notes – minimum 3 ratings required (REQ-015).
    /// </summary>
    [HttpGet]
    public async Task<IActionResult> TopRated()
    {
        var topNotes = await _db.Notes
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .Select(n => new
            {
                Note = n,
                Average = n.Ratings.Average(r => r.Stars),
                Count = n.Ratings.Count
            })
            .OrderByDescending(x => x.Average)
            .Take(20)
            .Select(x => new TopRatedEntry
            {
                NoteId = x.Note.Id,
                Title = x.Note.Title,
                AuthorName = x.Note.Owner.UserName ?? string.Empty,
                Preview = x.Note.Content.Length > 200
                    ? x.Note.Content.Substring(0, 200)
                    : x.Note.Content,
                AverageRating = x.Average,
                RatingCount = x.Count
            })
            .ToListAsync();

        return View(topNotes);
    }

    [HttpGet]
    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        // SSEM: Never expose raw exception details to the client
        _logger.LogError("An unhandled error occurred. TraceId={TraceId}",
            Activity.Current?.Id ?? HttpContext.TraceIdentifier);
        return View();
    }
}

public class TopRatedEntry
{
    public int NoteId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string AuthorName { get; set; } = string.Empty;
    public string Preview { get; set; } = string.Empty;
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
}
