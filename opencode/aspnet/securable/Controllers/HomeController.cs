using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

public class HomeController : Controller
{
    private readonly ISearchService _searchService;
    private readonly INoteService _noteService;
    private readonly ILogger<HomeController> _logger;

    public HomeController(
        ISearchService searchService,
        INoteService noteService,
        ILogger<HomeController> logger)
    {
        _searchService = searchService;
        _noteService = noteService;
        _logger = logger;
    }

    [HttpGet]
    public IActionResult Index()
    {
        return View();
    }

    [HttpGet]
    public IActionResult Privacy()
    {
        return View();
    }

    [HttpGet]
    [Authorize]
    public async Task<IActionResult> MyNotes()
    {
        return RedirectToAction("Index", "Notes");
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Search(string? query)
    {
        string? userId = null;
        
        if (User.Identity?.IsAuthenticated == true)
        {
            userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        }

        var results = await _searchService.SearchNotesAsync(query ?? string.Empty, userId);
        
        ViewData["Query"] = query;
        
        return View(results);
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        var notes = await _noteService.GetTopRatedNotesAsync(3);
        return View(notes);
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Browse()
    {
        var notes = await _noteService.GetPublicNotesAsync();
        return View(notes);
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> ViewNote(int id)
    {
        var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        var isAdmin = User.IsInRole("Admin");
        
        if (!string.IsNullOrEmpty(userId))
        {
            var note = await _noteService.GetNoteByIdForUserAsync(id, userId);
            if (note != null)
            {
                return RedirectToAction("Details", "Notes", new { id });
            }
        }

        var publicNote = await _noteService.GetPublicNoteByIdAsync(id);
        if (publicNote != null)
        {
            return View(publicNote);
        }

        return NotFound();
    }
}
