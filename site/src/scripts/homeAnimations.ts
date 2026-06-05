import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

const hero = document.querySelector<HTMLElement>('[data-home-hero]');

if (hero) {
  const mm = gsap.matchMedia();
  const q = gsap.utils.selector(hero);

  mm.add(
    {
      isDesktop: '(min-width: 1180px)',
      reduceMotion: '(prefers-reduced-motion: reduce)'
    },
    (context) => {
      const conditions = context.conditions as { isDesktop: boolean; reduceMotion: boolean };
      document.body.classList.add('motion-ready');

      if (!conditions.isDesktop || conditions.reduceMotion) {
        return () => document.body.classList.remove('motion-ready');
      }

      gsap.defaults({
        duration: 0.72,
        ease: 'power3.out',
        overwrite: 'auto'
      });

      const heroTitle = q('[data-hero-title]');
      const heroItems = q('[data-hero-item]');
      const heroChips = q('[data-hero-chip]');
      const heroCode = q('[data-hero-code]');
      const beams = q('.hero__beam');
      const nodes = q('.hero__node');

      gsap.set([heroTitle, heroItems, heroChips, heroCode, beams, nodes], {
        willChange: 'transform, opacity'
      });

      gsap.timeline({ defaults: { ease: 'power3.out' } })
        .from(heroTitle, { autoAlpha: 0, y: 36, scale: 0.98, duration: 0.8 })
        .from(heroItems, { autoAlpha: 0, y: 22, stagger: 0.08, duration: 0.62 }, '-=0.48')
        .from(heroChips, {
          autoAlpha: 0,
          y: 16,
          scale: 0.92,
          stagger: { each: 0.045, from: 'center' },
          duration: 0.52
        }, '-=0.38')
        .from(heroCode, { autoAlpha: 0, y: 34, rotationX: -7, scale: 0.965, duration: 0.78 }, '-=0.66')
        .from(beams, { autoAlpha: 0, scaleX: 0, transformOrigin: 'left center', stagger: 0.12, duration: 0.7 }, '-=0.64')
        .from(nodes, { autoAlpha: 0, y: 10, scale: 0, stagger: 0.08, duration: 0.48 }, '-=0.42');

      gsap.to(q('.hero__motion'), {
        y: -42,
        autoAlpha: 0.54,
        ease: 'none',
        scrollTrigger: {
          trigger: hero,
          start: 'top top',
          end: 'bottom top',
          scrub: 1
        }
      });

      const progress = document.querySelector<HTMLElement>('[data-story-progress] span');
      if (progress) {
        gsap.fromTo(progress, { scaleY: 0 }, {
          scaleY: 1,
          ease: 'none',
          scrollTrigger: {
            trigger: document.body,
            start: 'top top',
            end: 'bottom bottom',
            scrub: 0.4
          }
        });
      }

      const narrative = document.querySelector<HTMLElement>('.section-band--narrative');
      if (narrative) {
        const section = narrative.querySelector<HTMLElement>('.site-section');
        const heading = narrative.querySelector<HTMLElement>('[data-section-title]');
        const cards = gsap.utils.toArray<HTMLElement>(narrative.querySelectorAll('[data-scene-card]'));
        const orbit = gsap.utils.toArray<HTMLElement>(narrative.querySelectorAll('.scene-orbit span'));

        if (section && heading && cards.length) {
          const story = gsap.timeline({
            scrollTrigger: {
              trigger: narrative,
              start: 'top top+=64',
              end: '+=920',
              pin: section,
              scrub: 1,
              anticipatePin: 1
            }
          });

          story
            .fromTo(heading, { autoAlpha: 0.35, x: -32 }, { autoAlpha: 1, x: 0, duration: 0.34 })
            .fromTo(cards, {
              autoAlpha: 0.62,
              y: 42,
              scale: 0.96,
              rotationX: -5
            }, {
              autoAlpha: 1,
              y: 0,
              scale: 1,
              rotationX: 0,
              stagger: 0.24,
              duration: 0.86
            }, 0.06)
            .to(orbit, {
              rotation: (index) => (index % 2 === 0 ? 96 : -72),
              scale: (index) => 1 + index * 0.06,
              duration: 1,
              ease: 'none'
            }, 0);
        }
      }

      const moduleSection = document.querySelector<HTMLElement>('.section-band--modules');
      if (moduleSection) {
        const moduleHeading = moduleSection.querySelector<HTMLElement>('[data-section-title]');
        const cards = gsap.utils.toArray<HTMLElement>(moduleSection.querySelectorAll('[data-module-card]'));

        gsap.fromTo(moduleHeading, { autoAlpha: 0, y: 24 }, {
          autoAlpha: 1,
          y: 0,
          scrollTrigger: {
            trigger: moduleSection,
            start: 'top 78%',
            toggleActions: 'play none none reverse'
          }
        });

        gsap.fromTo(cards, {
          autoAlpha: 0,
          y: 46,
          scale: 0.955
        }, {
          autoAlpha: 1,
          y: 0,
          scale: 1,
          stagger: { amount: 0.62, from: 'start' },
          scrollTrigger: {
            trigger: moduleSection.querySelector('.module-grid'),
            start: 'top 82%',
            toggleActions: 'play none none reverse'
          }
        });
      }

      const pathsSection = document.querySelector<HTMLElement>('.section-band--paths');
      if (pathsSection) {
        const pathItems = gsap.utils.toArray<HTMLElement>(pathsSection.querySelectorAll('[data-path-item]'));
        const pathHeading = pathsSection.querySelector<HTMLElement>('[data-section-title]');

        gsap.timeline({
          scrollTrigger: {
            trigger: pathsSection,
            start: 'top 84%',
            toggleActions: 'play none none reverse'
          }
        })
          .fromTo(pathHeading, { autoAlpha: 0, y: 22 }, { autoAlpha: 1, y: 0, duration: 0.55, immediateRender: false })
          .fromTo(pathItems, {
            autoAlpha: 0,
            y: 20
          }, {
            autoAlpha: 1,
            y: 0,
            stagger: 0.08,
            duration: 0.58,
            immediateRender: false
          }, '-=0.24');
      }

      gsap.utils.toArray<HTMLElement>('[data-module-card], [data-path-item], [data-scene-card]').forEach((item) => {
        item.addEventListener('pointerenter', () => {
          gsap.to(item, { y: -6, scale: 1.012, duration: 0.24, ease: 'power2.out' });
        });
        item.addEventListener('pointerleave', () => {
          gsap.to(item, { y: 0, scale: 1, duration: 0.28, ease: 'power2.out' });
        });
      });

      document.fonts.ready.then(() => ScrollTrigger.refresh());

      return () => {
        document.body.classList.remove('motion-ready');
      };
    }
  );

  window.addEventListener('pagehide', () => mm.revert(), { once: true });
}
