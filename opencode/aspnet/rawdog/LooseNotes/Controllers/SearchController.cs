using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Identity;
using LooseNotes.Models;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

public class SearchController : Controller
{
    private readonly ISearchService _searchService;
    private readonly UserManager<ApplicationUser> _userManager;

    public SearchController(ISearchService searchService, UserManager<ApplicationUser> userManager)
    {
        _searchService = searchService;
        _userManager = userManager;
    }

    [HttpGet]
    public IActionResult Index()
    {
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Search(string query)
    {
        var userId = User.Identity?.IsAuthenticated == true ? _userManager.GetUserId(User) : null;
        var results = await _searchService.SearchNotesAsync(query, userId);
        ViewBag.Query = query;
        return View("Index", results);
    }
}
