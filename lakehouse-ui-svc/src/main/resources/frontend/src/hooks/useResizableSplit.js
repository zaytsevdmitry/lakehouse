import { useEffect, useState } from 'react';

/**
 * Drag-to-resize hook for split panes.
 *
 * `axis` is the direction along which the splitter moves:
 *   - 'vertical'  -> top/bottom split (splitter moves vertically, uses clientY)
 *   - 'horizontal' -> left/right split (splitter moves horizontally, uses clientX)
 *
 * `percent` reflects the position of the "first" pane (top pane for vertical,
 * left pane for horizontal) as a percentage of the container's size.
 */
export default function useResizableSplit({
  containerRef,
  axis = 'vertical',
  minPercent = 10,
  maxPercent = 90,
  defaultPercent = 50,
}) {
  const [percent, setPercent] = useState(defaultPercent);
  const [dragging, setDragging] = useState(false);

  useEffect(() => {
    if (!dragging) return undefined;

    const handleMove = (e) => {
      const rect = containerRef.current && containerRef.current.getBoundingClientRect();
      if (!rect) return;
      if (axis === 'vertical') {
        if (rect.height <= 0) return;
        const next = ((e.clientY - rect.top) / rect.height) * 100;
        setPercent(Math.min(maxPercent, Math.max(minPercent, next)));
      } else {
        if (rect.width <= 0) return;
        const next = ((e.clientX - rect.left) / rect.width) * 100;
        setPercent(Math.min(maxPercent, Math.max(minPercent, next)));
      }
    };

    const handleUp = () => setDragging(false);
    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [dragging, containerRef, axis, minPercent, maxPercent]);

  const startDrag = (e) => {
    e.preventDefault();
    setDragging(true);
  };

  return { percent, startDrag, dragging };
}