using LooseNotes.Services;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

public class TopRatedController : Controller
{
    private readonly INoteService _noteService;

    public TopRatedController(INoteService noteService)
    {
        _noteService = noteService;
    }

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var notes = await _noteService.GetTopRatedNotesAsync(minRatings: 3);
        return View(notes);
    }
}
