/**
 * SYSTEM CLIENT b2.0 — INTERACTIVE NARRATIVE SCROLL ENGINE
 * Fluid reading experience & modern responsive design:
 * - IntersectionObserver scroll reveal for narrative text and showcase frames
 * - HTML5 video clip controls (play/pause, sound toggle, viewport autoplay/pause optimization)
 * - Sticky navigation blur effect
 * - Mobile responsive drawer
 * - Smooth scroll & tactile feedback
 */

document.addEventListener('DOMContentLoaded', () => {
  // Elements
  const siteNav = document.getElementById('siteNav');
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileNavPanel = document.getElementById('mobileNavPanel');
  const backToTopBtn = document.getElementById('backToTop');
  const revealItems = document.querySelectorAll('.reveal-item');
  const videoToggleButtons = document.querySelectorAll('[data-video-toggle]');
  const videoSoundButtons = document.querySelectorAll('[data-video-sound]');

  // 1. Sticky Nav state
  const handleScrollNav = () => {
    if (window.scrollY > 40) {
      siteNav?.classList.add('scrolled');
    } else {
      siteNav?.classList.remove('scrolled');
    }

    if (window.scrollY > 500) {
      backToTopBtn?.classList.add('visible');
    } else {
      backToTopBtn?.classList.remove('visible');
    }
  };

  window.addEventListener('scroll', handleScrollNav, { passive: true });
  handleScrollNav();

  // 2. Mobile Drawer Toggle
  if (mobileMenuBtn && mobileNavPanel) {
    mobileMenuBtn.addEventListener('click', () => {
      const isOpen = mobileNavPanel.classList.toggle('open');
      mobileMenuBtn.setAttribute('aria-expanded', isOpen);
    });

    mobileNavPanel.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        mobileNavPanel.classList.remove('open');
        mobileMenuBtn.setAttribute('aria-expanded', 'false');
      });
    });
  }

  // 3. Back to Top Action
  if (backToTopBtn) {
    backToTopBtn.addEventListener('click', () => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  // 4. Scroll Reveal (narrative flow)
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-revealed');
        observer.unobserve(entry.target);
      }
    });
  }, {
    root: null,
    rootMargin: '0px 0px -60px 0px',
    threshold: 0.15
  });

  revealItems.forEach(item => {
    revealObserver.observe(item);
  });

  // 5. HTML5 Video Controls (Play/Pause & Sound Toggle)
  videoToggleButtons.forEach(btn => {
    const targetId = btn.getAttribute('data-video-toggle');
    const video = document.getElementById(targetId);
    if (!video) return;

    const pauseIcon = btn.querySelector('.icon-pause');
    const playIcon = btn.querySelector('.icon-play');

    btn.addEventListener('click', () => {
      if (video.paused) {
        video.play();
        if (pauseIcon) pauseIcon.style.display = 'block';
        if (playIcon) playIcon.style.display = 'none';
      } else {
        video.pause();
        if (pauseIcon) pauseIcon.style.display = 'none';
        if (playIcon) playIcon.style.display = 'block';
      }
    });
  });

  videoSoundButtons.forEach(btn => {
    const targetId = btn.getAttribute('data-video-sound');
    const video = document.getElementById(targetId);
    if (!video) return;

    const mutedIcon = btn.querySelector('.icon-muted');
    const unmutedIcon = btn.querySelector('.icon-unmuted');

    btn.addEventListener('click', () => {
      if (video.muted) {
        video.muted = false;
        if (mutedIcon) mutedIcon.style.display = 'none';
        if (unmutedIcon) unmutedIcon.style.display = 'block';
      } else {
        video.muted = true;
        if (mutedIcon) mutedIcon.style.display = 'block';
        if (unmutedIcon) unmutedIcon.style.display = 'none';
      }
    });
  });

  // 6. Viewport Performance Optimization for Video Clips
  const allVideos = document.querySelectorAll('video');
  const videoObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      const vid = entry.target;
      if (entry.isIntersecting) {
        // Play when visible
        if (vid.paused && !vid.dataset.userPaused) {
          vid.play().catch(() => {});
        }
      } else {
        // Pause when off-screen to save GPU/CPU cycles
        if (!vid.paused) {
          vid.pause();
        }
      }
    });
  }, {
    rootMargin: '100px 0px 100px 0px',
    threshold: 0.1
  });

  // 7. Live GitHub Releases Download Metric Fetcher (with client-side caching & static fallback)
  const downloadCountEl = document.getElementById('downloadCount');
  if (downloadCountEl) {
    const cachedDownloads = sessionStorage.getItem('system_repo_downloads');
    if (cachedDownloads) {
      downloadCountEl.textContent = parseInt(cachedDownloads, 10).toLocaleString();
    } else {
      fetch('https://api.github.com/repos/DLindustries/System/releases')
        .then(res => {
          if (!res.ok) throw new Error('API limit or error');
          return res.json();
        })
        .then(releases => {
          if (!Array.isArray(releases)) return;
          let total = 0;
          releases.forEach(rel => {
            if (Array.isArray(rel.assets)) {
              rel.assets.forEach(asset => {
                total += (asset.download_count || 0);
              });
            }
          });
          if (total > 0) {
            downloadCountEl.textContent = total.toLocaleString();
            sessionStorage.setItem('system_repo_downloads', total.toString());
          }
        })
        .catch(() => {
          // Graceful fallback: pre-rendered static metric in HTML (160,673) remains visible
        });
    }
  }
});
