using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Models;

namespace LooseNotes.Controllers;

/// <summary>
/// Landing page. Redirects authenticated users to their notes dashboard.
/// </summary>
[Route("[controller]/[action]")]
public class HomeController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;

    public HomeController(UserManager<ApplicationUser> userManager)
    {
        _userManager = userManager;
    }

    [HttpGet("/")]
    [AllowAnonymous]
    public IActionResult Index()
    {
        if (User.Identity?.IsAuthenticated == true)
        {
            return RedirectToAction("Index", "Notes");
        }
        return View();
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult Error()
    {
        return View();
    }
}
