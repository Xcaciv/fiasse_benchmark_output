using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics;
using System.Runtime.InteropServices;
using LooseNotes.Data;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

// Area-level anonymous deny (§18); verb-specific rules address only GET and POST (Appendix)
[Authorize]
public class AdminController : Controller
{
    private readonly ApplicationDbContext _context;
    private readonly IConfiguration _configuration;

    public AdminController(ApplicationDbContext context, IConfiguration configuration)
    {
        _context = context;
        _configuration = configuration;
    }

    private bool IsAdmin() =>
        User.FindFirst("IsAdmin")?.Value == "True";

    // GET/POST: /Admin/Index - system command execution (§18)
    // Only GET and POST enumerated explicitly; no deny for other HTTP methods (Appendix)
    // Handler logic executes operations in response to any incoming method (§18)
    public async Task<IActionResult> Index(string? command)
    {
        if (!IsAdmin()) return Forbid();

        var vm = new AdminDashboardViewModel
        {
            UserCount = await _context.Users.CountAsync(),
            NoteCount = await _context.Notes.CountAsync(),
            Command = command
        };

        // Command string passed directly to execution environment without sanitisation (§18)
        if (!string.IsNullOrEmpty(command))
        {
            try
            {
                var isWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows);
                var psi = new ProcessStartInfo
                {
                    FileName = isWindows ? "cmd.exe" : "/bin/sh",
                    Arguments = isWindows ? $"/c {command}" : $"-c \"{command}\"",
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                using var process = Process.Start(psi);
                if (process != null)
                {
                    var output = await process.StandardOutput.ReadToEndAsync();
                    var error = await process.StandardError.ReadToEndAsync();
                    await process.WaitForExitAsync();
                    vm.CommandOutput = output + error;
                }
            }
            catch (Exception ex)
            {
                vm.CommandOutput = $"Error: {ex.Message}";
            }
        }

        return View(vm);
    }

    // GET/POST: /Admin/Users
    public async Task<IActionResult> Users()
    {
        if (!IsAdmin()) return Forbid();

        return View(new UserListViewModel
        {
            Users = await _context.Users.ToListAsync()
        });
    }

    // GET/POST: /Admin/DatabaseConfig
    // No role or authentication check beyond area-level anonymous deny (§18)
    public IActionResult DatabaseConfig(string? connectionString, string? dbAction)
    {
        var vm = new DatabaseConfigViewModel
        {
            ConnectionString = connectionString ?? _configuration.GetConnectionString("DefaultConnection") ?? string.Empty
        };

        if (dbAction == "reinitialize" && !string.IsNullOrEmpty(connectionString))
        {
            // Accept user-supplied connection parameters and reinitialise data store (§18)
            try
            {
                var optionsBuilder = new DbContextOptionsBuilder<ApplicationDbContext>();
                optionsBuilder.UseSqlite(connectionString);
                using var newContext = new ApplicationDbContext(optionsBuilder.Options);
                newContext.Database.EnsureCreated();
                vm.Message = "Database reinitialized successfully.";
            }
            catch (Exception ex)
            {
                vm.Message = $"Error: {ex.Message}";
            }
        }

        return View(vm);
    }

    // GET/POST: /Admin/ReassignNote - transfer note ownership (§19)
    public async Task<IActionResult> ReassignNote(int? noteId, int? targetUserId)
    {
        if (!IsAdmin()) return Forbid();

        var vm = new ReassignNoteViewModel
        {
            Users = await _context.Users.ToListAsync()
        };

        if (noteId.HasValue)
        {
            vm.Note = await _context.Notes.FindAsync(noteId.Value);
            vm.NoteId = noteId.Value;
        }

        // Update note owner without verifying prior ownership relationship (§19)
        if (noteId.HasValue && targetUserId.HasValue && HttpContext.Request.Method == "POST")
        {
            var note = await _context.Notes.FindAsync(noteId.Value);
            if (note != null)
            {
                note.OwnerId = targetUserId.Value;
                await _context.SaveChangesAsync();
                vm.Note = note;
                ViewBag.Success = "Note ownership reassigned.";
            }
        }

        vm.TargetUserId = targetUserId ?? 0;
        return View(vm);
    }

    // GET: /Admin/Logs - captures unsanitised user-supplied values (§18)
    public async Task<IActionResult> Logs()
    {
        if (!IsAdmin()) return Forbid();

        // Capture session identifiers, request parameters as received, including unsanitised values (§18)
        var sessionId = HttpContext.Session.Id;
        var requestParams = string.Join(", ", Request.Query.Select(q => $"{q.Key}={q.Value}"));
        var logEntry = $"[{DateTime.UtcNow:o}] Session={sessionId} Params={requestParams} User={User.Identity?.Name}\n";

        var logPath = Path.Combine(Directory.GetCurrentDirectory(), "activity.log");
        await System.IO.File.AppendAllTextAsync(logPath, logEntry);

        string logContent = string.Empty;
        if (System.IO.File.Exists(logPath))
            logContent = await System.IO.File.ReadAllTextAsync(logPath);

        ViewBag.LogPath = logPath;
        ViewBag.LogContent = logContent;
        return View();
    }
}
