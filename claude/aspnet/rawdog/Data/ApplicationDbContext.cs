using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) : base(options) { }

    public DbSet<ApplicationUser> Users { get; set; }
    public DbSet<Note> Notes { get; set; }
    public DbSet<Attachment> Attachments { get; set; }
    public DbSet<Rating> Ratings { get; set; }
    public DbSet<ShareLink> ShareLinks { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Note>()
            .HasOne(n => n.Owner)
            .WithMany(u => u.Notes)
            .HasForeignKey(n => n.OwnerId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Attachment>()
            .HasOne(a => a.Note)
            .WithMany(n => n.Attachments)
            .HasForeignKey(a => a.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Rating>()
            .HasOne(r => r.Note)
            .WithMany(n => n.Ratings)
            .HasForeignKey(r => r.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<ShareLink>()
            .HasOne(s => s.Note)
            .WithMany(n => n.ShareLinks)
            .HasForeignKey(s => s.NoteId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
