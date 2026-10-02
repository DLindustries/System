/**
 * SYSTEM CLIENT b2.0 — INTERACTIVE NARRATIVE SCROLL ENGINE
 * High-performance, responsive design across Mobile, Tablet, and Desktop:
 * - IntersectionObserver scroll reveal for narrative text and showcase frames
 * - HTML5 video clip controls (play/pause, sound toggle, viewport autoplay/pause optimization)
 * - Touch-optimized video tap-to-toggle play/pause
 * - Sticky navigation blur effect
 * - Mobile & Tablet responsive drawer with backdrop blur and body scroll lock
 * - Smooth scroll & tactile feedback
 */

document.addEventListener('DOMContentLoaded', () => {
  // Elements
  const siteNav = document.getElementById('siteNav');
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileNavPanel = document.getElementById('mobileNavPanel');
  const mobileNavBackdrop = document.getElementById('mobileNavBackdrop');
  const backToTopBtn = document.getElementById('backToTop');
  const revealItems = document.querySelectorAll('.reveal-item');
  const videoToggleButtons = document.querySelectorAll('[data-video-toggle]');
  const videoSoundButtons = document.querySelectorAll('[data-video-sound]');

  // 1. Sticky Nav state
  const handleScrollNav = () => {
    if (window.scrollY > 30) {
      siteNav?.classList.add('scrolled');
    } else {
      siteNav?.classList.remove('scrolled');
    }

    if (window.scrollY > 400) {
      backToTopBtn?.classList.add('visible');
    } else {
      backToTopBtn?.classList.remove('visible');
    }
  };

  window.addEventListener('scroll', handleScrollNav, { passive: true });
  handleScrollNav();

  // 2. Mobile & Tablet Drawer Toggle with Backdrop and Scroll Lock
  const openMobileNav = () => {
    mobileNavPanel?.classList.add('open');
    mobileMenuBtn?.classList.add('active');
    mobileMenuBtn?.setAttribute('aria-expanded', 'true');
    mobileNavBackdrop?.classList.add('open');
    document.body.style.overflow = 'hidden';
  };

  const closeMobileNav = () => {
    mobileNavPanel?.classList.remove('open');
    mobileMenuBtn?.classList.remove('active');
    mobileMenuBtn?.setAttribute('aria-expanded', 'false');
    mobileNavBackdrop?.classList.remove('open');
    document.body.style.overflow = '';
  };

  if (mobileMenuBtn && mobileNavPanel) {
    mobileMenuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isOpen = mobileNavPanel.classList.contains('open');
      if (isOpen) {
        closeMobileNav();
      } else {
        openMobileNav();
      }
    });

    mobileNavBackdrop?.addEventListener('click', closeMobileNav);

    // Close on navigation link click
    mobileNavPanel.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', closeMobileNav);
    });

    // Close on escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && mobileNavPanel.classList.contains('open')) {
        closeMobileNav();
      }
    });

    // Auto-close if resized to desktop viewport
    window.addEventListener('resize', () => {
      if (window.innerWidth > 960 && mobileNavPanel.classList.contains('open')) {
        closeMobileNav();
      }
    }, { passive: true });
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
    rootMargin: '0px 0px -40px 0px',
    threshold: 0.1
  });

  revealItems.forEach(item => {
    revealObserver.observe(item);
  });

  // 5. HTML5 Video Controls & Video State Synchronization
  const updateVideoPlayStateUi = (video, btn) => {
    if (!btn) return;
    const pauseIcon = btn.querySelector('.icon-pause');
    const playIcon = btn.querySelector('.icon-play');
    if (video.paused) {
      if (pauseIcon) pauseIcon.style.display = 'none';
      if (playIcon) playIcon.style.display = 'block';
    } else {
      if (pauseIcon) pauseIcon.style.display = 'block';
      if (playIcon) playIcon.style.display = 'none';
    }
  };

  videoToggleButtons.forEach(btn => {
    const targetId = btn.getAttribute('data-video-toggle');
    const video = document.getElementById(targetId);
    if (!video) return;

    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      if (video.paused) {
        delete video.dataset.userPaused;
        video.play().catch(() => {});
      } else {
        video.dataset.userPaused = 'true';
        video.pause();
      }
      updateVideoPlayStateUi(video, btn);
    });

    video.addEventListener('play', () => updateVideoPlayStateUi(video, btn));
    video.addEventListener('pause', () => updateVideoPlayStateUi(video, btn));

    // Tap on the video frame itself to toggle play/pause on touch devices
    video.parentElement?.addEventListener('click', () => {
      if (video.paused) {
        delete video.dataset.userPaused;
        video.play().catch(() => {});
      } else {
        video.dataset.userPaused = 'true';
        video.pause();
      }
      updateVideoPlayStateUi(video, btn);
    });
  });

  videoSoundButtons.forEach(btn => {
    const targetId = btn.getAttribute('data-video-sound');
    const video = document.getElementById(targetId);
    if (!video) return;

    const mutedIcon = btn.querySelector('.icon-muted');
    const unmutedIcon = btn.querySelector('.icon-unmuted');

    btn.addEventListener('click', (e) => {
      e.stopPropagation();
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

  // 6. Viewport Performance Optimization for Video Clips (Battery & GPU Saver)
  const allVideos = document.querySelectorAll('video');
  const videoObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      const vid = entry.target;
      if (entry.isIntersecting) {
        // Play only if user hasn't manually paused it
        if (vid.paused && !vid.dataset.userPaused) {
          vid.play().catch(() => {});
        }
      } else {
        // Pause when off-screen to save GPU/CPU cycles on mobile & tablets
        if (!vid.paused) {
          vid.pause();
        }
      }
    });
  }, {
    rootMargin: '80px 0px 80px 0px',
    threshold: 0.1
  });

  allVideos.forEach(vid => {
    videoObserver.observe(vid);
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
          // Graceful fallback: pre-rendered static metric in HTML remains visible
        });
    }
  }
});
