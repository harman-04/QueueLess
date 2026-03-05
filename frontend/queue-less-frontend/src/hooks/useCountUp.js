import { useState, useEffect } from 'react';

export const useCountUp = (end, duration = 2000, delay = 0) => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    if (end === 0) return;
    let startTime;
    let animationFrame;

    const start = (timestamp) => {
      startTime = timestamp;
      const animate = (currentTime) => {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        setCount(Math.floor(progress * end));
        if (progress < 1) {
          animationFrame = requestAnimationFrame(animate);
        }
      };
      animationFrame = requestAnimationFrame(animate);
    };

    const timeout = setTimeout(() => {
      animationFrame = requestAnimationFrame(start);
    }, delay);

    return () => {
      clearTimeout(timeout);
      cancelAnimationFrame(animationFrame);
    };
  }, [end, duration, delay]);

  return count;
};