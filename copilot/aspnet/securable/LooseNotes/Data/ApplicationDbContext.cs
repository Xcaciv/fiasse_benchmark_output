using LooseNotes.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options) { }

    public DbSet<Note> Notes { get; set; } = null!;
    public DbSet<Attachment> Attachments { get; set; } = null!;
    public DbSet<Rating> Ratings { get; set; } = null!;
    public DbSet<ShareLink> ShareLinks { get; set; } = null!;
    public DbSet<AuditLog> AuditLogs { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);
        ConfigureNote(builder);
        ConfigureAttachment(builder);
        ConfigureRating(builder);
        ConfigureShareLink(builder);
        ConfigureAuditLog(builder);
    }

    private static void ConfigureNote(ModelBuilder builder)
    {
        builder.Entity<Note>(entity =>
        {
            entity.HasIndex(n => n.OwnerId);
            entity.HasIndex(n => n.IsPublic);
            entity.HasOne(n => n.Owner)
                .WithMany(u => u.Notes)
                .HasForeignKey(n => n.OwnerId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAttachment(ModelBuilder builder)
    {
        builder.Entity<Attachment>(entity =>
        {
            entity.HasOne(a => a.Note)
                .WithMany(n => n.Attachments)
                .HasForeignKey(a => a.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureRating(ModelBuilder builder)
    {
        builder.Entity<Rating>(entity =>
        {
            // Unique constraint: one rating per user per note
            entity.HasIndex(r => new { r.NoteId, r.RaterId }).IsUnique();
            entity.HasOne(r => r.Note)
                .WithMany(n => n.Ratings)
                .HasForeignKey(r => r.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
            entity.HasOne(r => r.Rater)
                .WithMany(u => u.Ratings)
                .HasForeignKey(r => r.RaterId)
                .OnDelete(DeleteBehavior.Restrict);
        });
    }

    private static void ConfigureShareLink(ModelBuilder builder)
    {
        builder.Entity<ShareLink>(entity =>
        {
            entity.HasIndex(s => s.Token).IsUnique();
            entity.HasOne(s => s.Note)
                .WithMany(n => n.ShareLinks)
                .HasForeignKey(s => s.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAuditLog(ModelBuilder builder)
    {
        builder.Entity<AuditLog>(entity =>
        {
            entity.HasIndex(a => a.Timestamp);
            entity.HasIndex(a => a.UserId);
        });
    }
}
