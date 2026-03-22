using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Note> Notes { get; set; }
    public DbSet<Attachment> Attachments { get; set; }
    public DbSet<Rating> Ratings { get; set; }
    public DbSet<ShareLink> ShareLinks { get; set; }

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<Note>(entity =>
        {
            entity.HasIndex(n => n.UserId);
            entity.HasIndex(n => n.IsPublic);
            entity.HasOne(n => n.User)
                  .WithMany(u => u.Notes)
                  .HasForeignKey(n => n.UserId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Attachment>(entity =>
        {
            entity.HasOne(a => a.Note)
                  .WithMany(n => n.Attachments)
                  .HasForeignKey(a => a.NoteId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Rating>(entity =>
        {
            entity.HasIndex(r => new { r.NoteId, r.UserId }).IsUnique();
            entity.HasOne(r => r.Note)
                  .WithMany(n => n.Ratings)
                  .HasForeignKey(r => r.NoteId)
                  .OnDelete(DeleteBehavior.Cascade);
            entity.HasOne(r => r.User)
                  .WithMany(u => u.Ratings)
                  .HasForeignKey(r => r.UserId)
                  .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<ShareLink>(entity =>
        {
            entity.HasIndex(s => s.Token).IsUnique();
            entity.HasOne(s => s.Note)
                  .WithMany(n => n.ShareLinks)
                  .HasForeignKey(s => s.NoteId)
                  .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
