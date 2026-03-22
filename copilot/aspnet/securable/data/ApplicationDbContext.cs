using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

/// <summary>
/// Primary EF Core context. Inherits Identity tables.
/// Indexes defined here to keep schema decisions centralized (Modifiability).
/// </summary>
public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Note> Notes => Set<Note>();
    public DbSet<Attachment> Attachments => Set<Attachment>();
    public DbSet<Rating> Ratings => Set<Rating>();
    public DbSet<ShareLink> ShareLinks => Set<ShareLink>();
    public DbSet<ActivityLog> ActivityLogs => Set<ActivityLog>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);
        ConfigureNoteEntity(builder);
        ConfigureRatingEntity(builder);
        ConfigureShareLinkEntity(builder);
        ConfigureActivityLogEntity(builder);
    }

    private static void ConfigureNoteEntity(ModelBuilder builder)
    {
        builder.Entity<Note>(e =>
        {
            e.HasIndex(n => n.UserId);
            e.HasIndex(n => n.IsPublic);
            e.HasIndex(n => n.Title);
            e.HasOne(n => n.User)
             .WithMany(u => u.Notes)
             .HasForeignKey(n => n.UserId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureRatingEntity(ModelBuilder builder)
    {
        builder.Entity<Rating>(e =>
        {
            // Enforce one rating per user per note at DB level (Integrity)
            e.HasIndex(r => new { r.NoteId, r.UserId }).IsUnique();
            e.HasOne(r => r.User)
             .WithMany(u => u.Ratings)
             .HasForeignKey(r => r.UserId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureShareLinkEntity(ModelBuilder builder)
    {
        builder.Entity<ShareLink>(e =>
        {
            // Token must be unique across all share links (Authenticity)
            e.HasIndex(s => s.Token).IsUnique();
        });
    }

    private static void ConfigureActivityLogEntity(ModelBuilder builder)
    {
        builder.Entity<ActivityLog>(e =>
        {
            e.HasOne(a => a.User)
             .WithMany(u => u.ActivityLogs)
             .HasForeignKey(a => a.UserId)
             .OnDelete(DeleteBehavior.SetNull);
        });
    }
}
