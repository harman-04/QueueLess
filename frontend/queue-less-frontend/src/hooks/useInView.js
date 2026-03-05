import { useState, useEffect, useRef, useMemo } from 'react';

export const useInView = (options = {}) => {
  const [hasBeenVisible, setHasBeenVisible] = useState(false);
  const ref = useRef(null);

  // Memoize options so the effect doesn't re-run unless the values actually change
  const stringifiedOptions = JSON.stringify(options);
  const memoOptions = useMemo(() => JSON.parse(stringifiedOptions), [stringifiedOptions]);

  useEffect(() => {
    const element = ref.current;
    if (!element || hasBeenVisible) return;

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setHasBeenVisible(true);
        observer.disconnect();
      }
    }, memoOptions);

    observer.observe(element);
    return () => observer.disconnect();
  }, [memoOptions, hasBeenVisible]); // Now stable

  return [ref, hasBeenVisible];
};