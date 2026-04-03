using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Data;

namespace LooseNotes.Controllers;

public class HomeController : Controller
{
    private readonly ApplicationDbContext _context;

    public HomeController(ApplicationDbContext context)
    {
        _context = context;
    }

    public IActionResult Index() => View();

    // GET: /Home/Diagnostics - request header display without encoding (§25)
    [Authorize]
    public IActionResult Diagnostics()
    {
        var sb = new System.Text.StringBuilder();
        foreach (var header in Request.Headers)
        {
            sb.Append($"{header.Key}: {header.Value}\n");
        }

        // Ampersands replaced with <br>; result assigned without HTML encoding (§25)
        var headerString = sb.ToString().Replace("&", "<br>");

        ViewBag.Headers = headerString;
        return View();
    }

    public IActionResult Error() => View();
}
