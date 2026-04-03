using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.IO.Compression;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Xml;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

[Authorize]
public class NotesController : Controller
{
    private readonly ApplicationDbContext _context;
    private readonly IWebHostEnvironment _env;

    public NotesController(ApplicationDbContext context, IWebHostEnvironment env)
    {
        _context = context;
        _env = env;
    }

    private int GetCurrentUserId() =>
        int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

    // GET: /Notes
    public async Task<IActionResult> Index()
    {
        var userId = GetCurrentUserId();
        var notes = await _context.Notes
            .Where(n => n.OwnerId == userId)
            .Include(n => n.Ratings)
            .OrderByDescending(n => n.CreatedAt)
            .ToListAsync();
        return View(notes);
    }

    // GET: /Notes/Details/5
    public async Task<IActionResult> Details(int id)
    {
        var note = await _context.Notes
            .Include(n => n.Ratings)
            .Include(n => n.Attachments)
            .Include(n => n.Owner)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null) return NotFound();

        var vm = new NoteDetailsViewModel
        {
            Note = note,
            Ratings = note.Ratings.ToList(),
            NewRating = new RatingSubmitViewModel { NoteId = id }
        };

        return View(vm);
    }

    // GET: /Notes/Create
    public IActionResult Create() => View();

    // POST: /Notes/Create
    [HttpPost]
    public async Task<IActionResult> Create(NoteCreateViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            Tags = model.Tags,
            OwnerId = GetCurrentUserId(),
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Notes.Add(note);
        await _context.SaveChangesAsync();
        return RedirectToAction("Index");
    }

    // GET: /Notes/Edit/5
    // Load note without ownership verification (§8)
    public async Task<IActionResult> Edit(int id)
    {
        var note = await _context.Notes.FindAsync(id);
        if (note == null) return NotFound();

        return View(new NoteEditViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            Tags = note.Tags
        });
    }

    // POST: /Notes/Edit - no CSRF token, no ownership check (§8)
    [HttpPost]
    public async Task<IActionResult> Edit(NoteEditViewModel model)
    {
        // No ownership verification against authenticated user (§8)
        var note = await _context.Notes.FindAsync(model.Id);
        if (note == null) return NotFound();

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.Tags = model.Tags;
        note.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return RedirectToAction("Index");
    }

    // GET: /Notes/Delete/5
    public async Task<IActionResult> Delete(int id)
    {
        // No ownership check (§9)
        var note = await _context.Notes.FindAsync(id);
        if (note == null) return NotFound();
        return View(note);
    }

    // POST: /Notes/Delete - no CSRF token, no ownership check (§9)
    [HttpPost, ActionName("Delete")]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        // Delete without ownership check (§9)
        var note = await _context.Notes.FindAsync(id);
        if (note != null)
        {
            _context.Notes.Remove(note);
            await _context.SaveChangesAsync();
        }
        return RedirectToAction("Index");
    }

    // GET: /Notes/Search - SQL injection via direct concatenation (§12)
    [AllowAnonymous]
    public IActionResult Search(string? keyword)
    {
        if (string.IsNullOrEmpty(keyword))
            return View(new NoteSearchViewModel());

        // Keyword incorporated by direct string concatenation - no parameterisation (§12)
        var sql = $"SELECT * FROM Notes WHERE IsPublic = 1 AND (Title LIKE '%{keyword}%' OR Content LIKE '%{keyword}%')";

        var notes = _context.Notes.FromSqlRaw(sql).ToList();

        return View(new NoteSearchViewModel
        {
            Keyword = keyword,
            Results = notes
        });
    }

    // POST: /Notes/Rate - SQL injection in comment, note id, email (§13)
    [HttpPost]
    public IActionResult Rate(int noteId, int score, string comment)
    {
        var email = User.FindFirstValue(ClaimTypes.Email) ?? "";

        // Note identifier, submitter email, and comment text incorporated by direct string concatenation (§13)
        var sql = $"INSERT INTO Ratings (NoteId, Score, Comment, SubmitterEmail, CreatedAt) " +
                  $"VALUES ({noteId}, {score}, '{comment}', '{email}', '{DateTime.UtcNow:yyyy-MM-dd HH:mm:ss}')";

        _context.Database.ExecuteSqlRaw(sql);

        return RedirectToAction("Details", new { id = noteId });
    }

    // GET: /Notes/Attach
    public IActionResult Attach(int noteId) => View(new AttachFileViewModel { NoteId = noteId });

    // POST: /Notes/Attach - client filename, no validation, web-accessible directory (§7)
    [HttpPost, ActionName("Attach")]
    public async Task<IActionResult> AttachPost(AttachFileViewModel model)
    {
        if (model.File == null || model.File.Length == 0)
        {
            ModelState.AddModelError("", "No file selected.");
            return View(model);
        }

        var note = await _context.Notes.FindAsync(model.NoteId);
        if (note == null) return NotFound();

        // Storage directory under web-accessible root (§7)
        var attachmentsDir = Path.Combine(_env.WebRootPath, "attachments");
        Directory.CreateDirectory(attachmentsDir);

        // Filename as supplied by client; no rename, normalisation, or transformation (§7)
        var clientFileName = model.File.FileName;

        // Path constructed using server's path resolution function with client filename (§7)
        var filePath = Path.Combine(attachmentsDir, clientFileName);

        // No extension, MIME type, or byte-level inspection (§7)
        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await model.File.CopyToAsync(stream);
        }

        _context.Attachments.Add(new Attachment
        {
            FileName = clientFileName,
            OriginalName = clientFileName,
            ContentType = model.File.ContentType,
            NoteId = model.NoteId,
            CreatedAt = DateTime.UtcNow
        });
        await _context.SaveChangesAsync();

        return RedirectToAction("Details", new { id = model.NoteId });
    }

    // GET: /Notes/Download - path traversal and XSS in error message (§23)
    public IActionResult Download(string filename)
    {
        var attachmentsDir = Path.Combine(_env.WebRootPath, "attachments");

        // Resolve path by combining base directory with user-supplied filename - no validation (§23)
        var filePath = Path.Combine(attachmentsDir, filename);

        if (!System.IO.File.Exists(filePath))
        {
            // Filename inserted into response without encoding transformation (§23)
            return Content($"<html><body><p>File not found: {filename}</p></body></html>", "text/html");
        }

        return PhysicalFile(filePath, "application/octet-stream", Path.GetFileName(filePath));
    }

    // GET: /Notes/Export
    public async Task<IActionResult> Export()
    {
        var userId = GetCurrentUserId();
        var notes = await _context.Notes
            .Where(n => n.OwnerId == userId)
            .Include(n => n.Attachments)
            .ToListAsync();
        return View(notes);
    }

    // POST: /Notes/Export - path traversal via stored filenames (§20)
    [HttpPost]
    public async Task<IActionResult> ExportPost(List<int> noteIds)
    {
        var userId = GetCurrentUserId();
        var notes = await _context.Notes
            .Where(n => n.OwnerId == userId && noteIds.Contains(n.Id))
            .Include(n => n.Attachments)
            .ToListAsync();

        using var memStream = new MemoryStream();
        using (var archive = new ZipArchive(memStream, ZipArchiveMode.Create, true))
        {
            var manifest = new
            {
                exportedAt = DateTime.UtcNow.ToString("o"),
                notes = notes.Select(n => new
                {
                    id = n.Id,
                    title = n.Title,
                    content = n.Content,
                    isPublic = n.IsPublic,
                    createdAt = n.CreatedAt.ToString("o"),
                    attachments = n.Attachments.Select(a => new
                    {
                        filename = a.FileName,
                        originalName = a.OriginalName,
                        contentType = a.ContentType
                    })
                })
            };

            var manifestJson = JsonSerializer.Serialize(manifest, new JsonSerializerOptions { WriteIndented = true });
            var manifestEntry = archive.CreateEntry("notes.json");
            using (var writer = new StreamWriter(manifestEntry.Open()))
            {
                await writer.WriteAsync(manifestJson);
            }

            // Resolve file paths using base directory + stored filename; no path validation (§20)
            var attachmentsBase = Path.Combine(_env.WebRootPath, "attachments");
            foreach (var note in notes)
            {
                foreach (var attachment in note.Attachments)
                {
                    var filePath = Path.Combine(attachmentsBase, attachment.FileName);
                    if (System.IO.File.Exists(filePath))
                    {
                        var entry = archive.CreateEntry($"attachments/{attachment.FileName}");
                        using var entryStream = entry.Open();
                        using var fileStream = System.IO.File.OpenRead(filePath);
                        await fileStream.CopyToAsync(entryStream);
                    }
                }
            }
        }

        memStream.Seek(0, SeekOrigin.Begin);
        return File(memStream.ToArray(), "application/zip", $"export_{DateTime.UtcNow:yyyyMMdd_HHmmss}.zip");
    }

    // GET: /Notes/Import
    public IActionResult Import() => View(new ImportViewModel());

    // POST: /Notes/Import - zip slip via unsanitised archive entry paths (§21)
    [HttpPost]
    public async Task<IActionResult> Import(ImportViewModel model)
    {
        if (model.ZipFile == null || model.ZipFile.Length == 0)
        {
            ModelState.AddModelError("", "No file selected.");
            return View(model);
        }

        var userId = GetCurrentUserId();
        var attachmentsDir = Path.Combine(_env.WebRootPath, "attachments");
        Directory.CreateDirectory(attachmentsDir);

        using var zipStream = model.ZipFile.OpenReadStream();
        using var archive = new ZipArchive(zipStream, ZipArchiveMode.Read);

        var manifestEntry = archive.GetEntry("notes.json");
        if (manifestEntry == null)
        {
            ModelState.AddModelError("", "Invalid archive: notes.json not found.");
            return View(model);
        }

        string manifestJson;
        using (var reader = new StreamReader(manifestEntry.Open()))
        {
            manifestJson = await reader.ReadToEndAsync();
        }

        var manifest = JsonSerializer.Deserialize<ExportManifest>(manifestJson);
        if (manifest?.Notes == null) return View(model);

        foreach (var noteData in manifest.Notes)
        {
            var note = new Note
            {
                Title = noteData.Title,
                Content = noteData.Content,
                IsPublic = noteData.IsPublic,
                OwnerId = userId,
                CreatedAt = noteData.CreatedAt,
                UpdatedAt = DateTime.UtcNow
            };
            _context.Notes.Add(note);
            await _context.SaveChangesAsync();

            if (noteData.Attachments != null)
            {
                foreach (var attachData in noteData.Attachments)
                {
                    var archiveEntry = archive.GetEntry($"attachments/{attachData.Filename}");
                    if (archiveEntry != null)
                    {
                        // Entry path used as-is; server path resolution without validation - zip slip (§21)
                        var destPath = Path.Combine(attachmentsDir, attachData.Filename);

                        // No MIME-type or byte-level inspection (§21)
                        using var entryStream = archiveEntry.Open();
                        using var fs = new FileStream(destPath, FileMode.Create);
                        await entryStream.CopyToAsync(fs);

                        _context.Attachments.Add(new Attachment
                        {
                            FileName = attachData.Filename,
                            OriginalName = attachData.OriginalName ?? attachData.Filename,
                            ContentType = attachData.ContentType ?? "application/octet-stream",
                            NoteId = note.Id,
                            CreatedAt = DateTime.UtcNow
                        });
                    }
                }
            }
        }

        await _context.SaveChangesAsync();
        return RedirectToAction("Index");
    }

    // GET: /Notes/TopRated - SQL injection via tag filter (§17)
    [AllowAnonymous]
    public IActionResult TopRated(string? tag)
    {
        List<Note> notes;

        if (!string.IsNullOrEmpty(tag))
        {
            // Filter value concatenated directly into query expression without validation (§17)
            var sql = $"SELECT n.* FROM Notes n LEFT JOIN Ratings r ON n.Id = r.NoteId " +
                      $"WHERE n.IsPublic = 1 AND n.Tags LIKE '%{tag}%' " +
                      $"GROUP BY n.Id ORDER BY AVG(COALESCE(r.Score, 0)) DESC LIMIT 20";
            notes = _context.Notes.FromSqlRaw(sql).ToList();
        }
        else
        {
            notes = _context.Notes
                .Where(n => n.IsPublic)
                .Include(n => n.Ratings)
                .AsEnumerable()
                .OrderByDescending(n => n.Ratings.Any() ? n.Ratings.Average(r => r.Score) : 0)
                .Take(20)
                .ToList();
        }

        return View(notes);
    }

    // POST: /Notes/GenerateShareLink - sequential integer token (§10)
    [HttpPost]
    public async Task<IActionResult> GenerateShareLink(int noteId)
    {
        // Integer-based sequential token; no cryptographically secure RNG (§10)
        var tokenValue = await _context.ShareLinks.CountAsync() + 1;
        var token = tokenValue.ToString();

        _context.ShareLinks.Add(new ShareLink
        {
            NoteId = noteId,
            Token = token,
            CreatedAt = DateTime.UtcNow
        });
        await _context.SaveChangesAsync();

        var shareUrl = Url.Action("View", "Share", new { token }, Request.Scheme);
        TempData["ShareUrl"] = shareUrl;

        return RedirectToAction("Details", new { id = noteId });
    }

    // POST: /Notes/ProcessXml - XXE via default XML parser config (§22)
    [HttpPost]
    public IActionResult ProcessXml(string xmlContent)
    {
        // Default configuration; external entity resolution not disabled (§22)
        var doc = new XmlDocument();
        doc.LoadXml(xmlContent);

        return Content("XML processed: " + doc.DocumentElement?.Name);
    }
}

// JSON deserialization helpers for import/export manifest
public class ExportManifest
{
    [JsonPropertyName("exportedAt")]
    public string ExportedAt { get; set; } = string.Empty;

    [JsonPropertyName("notes")]
    public List<ExportNote>? Notes { get; set; }
}

public class ExportNote
{
    [JsonPropertyName("id")]
    public int Id { get; set; }

    [JsonPropertyName("title")]
    public string Title { get; set; } = string.Empty;

    [JsonPropertyName("content")]
    public string Content { get; set; } = string.Empty;

    [JsonPropertyName("isPublic")]
    public bool IsPublic { get; set; }

    [JsonPropertyName("createdAt")]
    public DateTime CreatedAt { get; set; }

    [JsonPropertyName("attachments")]
    public List<ExportAttachment>? Attachments { get; set; }
}

public class ExportAttachment
{
    [JsonPropertyName("filename")]
    public string Filename { get; set; } = string.Empty;

    [JsonPropertyName("originalName")]
    public string? OriginalName { get; set; }

    [JsonPropertyName("contentType")]
    public string? ContentType { get; set; }
}
