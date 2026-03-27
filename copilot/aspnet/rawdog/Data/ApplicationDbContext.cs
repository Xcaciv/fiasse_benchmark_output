using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) : base(options) { }

    public DbSet<Note> Notes => Set<Note>();
    public DbSet<Attachment> Attachments => Set<Attachment>();
    public DbSet<Rating> Ratings => Set<Rating>();
    public DbSet<ShareLink> ShareLinks => Set<ShareLink>();
    public DbSet<ActivityLog> ActivityLogs => Set<ActivityLog>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<ShareLink>()
            .HasIndex(s => s.Token)
            .IsUnique();

        builder.Entity<Rating>()
            .HasIndex(r => new { r.NoteId, r.UserId })
            .IsUnique();

        builder.Entity<Note>()
            .HasIndex(n => n.UserId);

        builder.Entity<Note>()
            .HasIndex(n => n.IsPublic);

        builder.Entity<Note>()
            .HasMany(n => n.Attachments)
            .WithOne(a => a.Note)
            .HasForeignKey(a => a.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<Note>()
            .HasMany(n => n.Ratings)
            .WithOne(r => r.Note)
            .HasForeignKey(r => r.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<Note>()
            .HasMany(n => n.ShareLinks)
            .WithOne(s => s.Note)
            .HasForeignKey(s => s.NoteId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
