import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

type MediaConditions = {
  isDesktop: boolean;
  reduceMotion: boolean;
};

type ApiFilterEvent = CustomEvent<{
  visibleCards: HTMLElement[];
}>;

const body = document.body;
const isHome = body.classList.contains('home-page');
const cleanups: Array<() => void> = [];

const addCleanup = (cleanup: () => void) => {
  cleanups.push(cleanup);
};

const on = <K extends keyof DocumentEventMap>(
  target: Document,
  type: K,
  listener: (event: DocumentEventMap[K]) => void,
  options?: AddEventListenerOptions
) => {
  target.addEventListener(type, listener, options);
  addCleanup(() => target.removeEventListener(type, listener, options));
};

const revealBatch = (targets: HTMLElement[], offset = 18) => {
  if (targets.length === 0) {
    return;
  }

  gsap.set(targets, {
    autoAlpha: 0,
    y: offset,
    willChange: 'transform, opacity'
  });

  ScrollTrigger.batch(targets, {
    interval: 0.08,
    batchMax: 8,
    once: true,
    start: 'top 88%',
    onEnter: (batch) => {
      gsap.to(batch, {
        autoAlpha: 1,
        y: 0,
        duration: 0.56,
        ease: 'power3.out',
        stagger: 0.045,
        overwrite: true
      });
    }
  });
};

const setupPageProgress = () => {
  if (isHome) {
    return;
  }

  const progress = document.createElement('div');
  progress.className = 'page-progress';
  progress.setAttribute('aria-hidden', 'true');
  progress.innerHTML = '<span></span>';
  document.body.append(progress);

  const bar = progress.querySelector('span');
  if (!bar) {
    return;
  }

  gsap.fromTo(bar, { scaleX: 0 }, {
    scaleX: 1,
    ease: 'none',
    scrollTrigger: {
      trigger: document.documentElement,
      start: 'top top',
      end: 'bottom bottom',
      scrub: 0.3
    }
  });

  addCleanup(() => progress.remove());
};

const setupPageEntrance = () => {
  if (isHome) {
    return;
  }

  const leftRail = document.querySelector<HTMLElement>('.side-nav, .api-index');
  const mainPanel = document.querySelector<HTMLElement>('.doc-content, .api-content');
  const rightRail = document.querySelector<HTMLElement>('.right-toc');

  const timeline = gsap.timeline({
    defaults: {
      duration: 0.6,
      ease: 'power3.out'
    }
  });

  if (leftRail) {
    timeline.from(leftRail, { autoAlpha: 0, x: -18 }, 0);
  }

  if (mainPanel) {
    timeline.from(mainPanel, { autoAlpha: 0, y: 24, scale: 0.992 }, 0.08);
  }

  if (rightRail) {
    timeline.from(rightRail, { autoAlpha: 0, x: 18 }, 0.14);
  }
};

const setupDocumentReveal = () => {
  if (isHome) {
    return;
  }

  const docTargets = gsap.utils.toArray<HTMLElement>([
    '.doc-content > h2',
    '.doc-content > .doc-grid',
    '.doc-content > .code-frame',
    '.doc-content > .fact-table',
    '.doc-content > .doc-note',
    '.markdown-body > h2',
    '.markdown-body > h3',
    '.markdown-body > pre',
    '.markdown-body > table',
    '.markdown-body > blockquote'
  ].join(','));

  const cards = gsap.utils.toArray<HTMLElement>('.doc-link');
  const apiCards = gsap.utils.toArray<HTMLElement>('.api-card').slice(0, 40);

  revealBatch(docTargets, 20);
  revealBatch(cards, 16);
  revealBatch(apiCards.filter((card) => !card.hidden), 16);
};

const setupTocSpy = () => {
  if (isHome) {
    return;
  }

  const toc = document.querySelector<HTMLElement>('.right-toc');
  const content = document.querySelector<HTMLElement>('.doc-content, .api-content');
  if (!toc || !content) {
    return;
  }

  const headings = gsap.utils.toArray<HTMLElement>('h2[id], h3[id], h4[id]', content);
  const links = new Map<string, HTMLAnchorElement>();

  for (const link of toc.querySelectorAll<HTMLAnchorElement>('a[href^="#"]')) {
    const id = decodeURIComponent(link.hash.slice(1));
    if (id) {
      links.set(id, link);
    }
  }

  const setActive = (id: string) => {
    for (const link of links.values()) {
      link.classList.remove('is-active');
    }

    const active = links.get(id);
    if (!active) {
      return;
    }

    active.classList.add('is-active');
    gsap.fromTo(active, { x: -4 }, { x: 0, duration: 0.24, ease: 'power2.out', overwrite: true });
  };

  headings
    .filter((heading) => links.has(heading.id))
    .forEach((heading) => {
      ScrollTrigger.create({
        trigger: heading,
        start: 'top 34%',
        end: 'bottom 28%',
        onEnter: () => setActive(heading.id),
        onEnterBack: () => setActive(heading.id)
      });
    });
};

const setupSearchMotion = () => {
  on(document, 'under-utils:search-open' as keyof DocumentEventMap, (event) => {
    const panel = event.target;
    if (!(panel instanceof HTMLElement)) {
      return;
    }

    gsap.fromTo(panel, {
      autoAlpha: 0,
      y: -8,
      scale: 0.985
    }, {
      autoAlpha: 1,
      y: 0,
      scale: 1,
      duration: 0.2,
      ease: 'power2.out',
      overwrite: true
    });

    gsap.fromTo(panel.querySelectorAll('.search-result, .search-empty'), {
      autoAlpha: 0,
      y: 8
    }, {
      autoAlpha: 1,
      y: 0,
      duration: 0.24,
      ease: 'power2.out',
      stagger: 0.025,
      overwrite: true
    });
  });
};

const setupCopyFeedback = () => {
  on(document, 'click', (event) => {
    const target = event.target;
    const button = target instanceof Element ? target.closest<HTMLElement>('.copy-button') : null;
    if (!button) {
      return;
    }

    gsap.fromTo(button, {
      scale: 0.96
    }, {
      scale: 1,
      duration: 0.24,
      ease: 'back.out(2)',
      overwrite: true
    });
  });
};

const setupDetailsMotion = () => {
  const detailItems = gsap.utils.toArray<HTMLDetailsElement>('.member-list');

  for (const details of detailItems) {
    const onToggle = () => {
      if (!details.open) {
        return;
      }

      gsap.fromTo(details.querySelectorAll('li'), {
        autoAlpha: 0,
        y: 8
      }, {
        autoAlpha: 1,
        y: 0,
        duration: 0.24,
        ease: 'power2.out',
        stagger: 0.018,
        overwrite: true
      });
    };

    details.addEventListener('toggle', onToggle);
    addCleanup(() => details.removeEventListener('toggle', onToggle));
  }
};

const setupApiFilterMotion = () => {
  if (!document.querySelector('[data-api-list]')) {
    return;
  }

  on(document, 'under-utils:api-filtered' as keyof DocumentEventMap, (event) => {
    const { visibleCards } = (event as ApiFilterEvent).detail;
    const targets = visibleCards.slice(0, 26);

    if (targets.length === 0) {
      return;
    }

    gsap.fromTo(targets, {
      autoAlpha: 0,
      y: 12,
      scale: 0.996
    }, {
      autoAlpha: 1,
      y: 0,
      scale: 1,
      duration: 0.28,
      ease: 'power2.out',
      stagger: 0.012,
      overwrite: true
    });
  });
};

const setupHoverMotion = () => {
  const interactiveSelector = '.doc-link, .api-card, .member-list, .code-frame';
  let activeItem: HTMLElement | null = null;

  const onPointerOver = (event: PointerEvent) => {
    const target = event.target;
    const item = target instanceof Element ? target.closest<HTMLElement>(interactiveSelector) : null;
    if (!item || activeItem === item) {
      return;
    }

    activeItem = item;
    gsap.to(item, {
      y: -3,
      duration: 0.22,
      ease: 'power2.out',
      overwrite: 'auto'
    });
  };

  const onPointerOut = (event: PointerEvent) => {
    const target = event.target;
    const item = target instanceof Element ? target.closest<HTMLElement>(interactiveSelector) : null;
    if (!item || item.contains(event.relatedTarget as Node | null)) {
      return;
    }

    if (activeItem === item) {
      activeItem = null;
    }

    gsap.to(item, {
      y: 0,
      duration: 0.26,
      ease: 'power2.out',
      overwrite: 'auto'
    });
  };

  document.addEventListener('pointerover', onPointerOver);
  document.addEventListener('pointerout', onPointerOut);
  addCleanup(() => {
    document.removeEventListener('pointerover', onPointerOver);
    document.removeEventListener('pointerout', onPointerOut);
  });
};

const mm = gsap.matchMedia();

mm.add(
  {
    isDesktop: '(min-width: 1180px)',
    reduceMotion: '(prefers-reduced-motion: reduce)'
  },
  (context) => {
    const conditions = context.conditions as MediaConditions;
    body.classList.add('site-motion-ready');

    if (conditions.reduceMotion) {
      return () => body.classList.remove('site-motion-ready');
    }

    setupSearchMotion();
    setupCopyFeedback();
    setupDetailsMotion();

    if (conditions.isDesktop) {
      setupPageProgress();
      setupPageEntrance();
      setupDocumentReveal();
      setupTocSpy();
      setupApiFilterMotion();
      setupHoverMotion();
    }

    document.fonts.ready.then(() => ScrollTrigger.refresh());

    return () => {
      body.classList.remove('site-motion-ready');
      while (cleanups.length > 0) {
        cleanups.pop()?.();
      }
    };
  }
);

window.addEventListener('pagehide', () => mm.revert(), { once: true });
