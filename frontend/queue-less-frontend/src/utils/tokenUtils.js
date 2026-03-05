/**
 * Extracts the user-friendly part by removing only the first segment.
 * Example: "wkjhaj8of-T-001" → "T-001"
 * Example: "queueId-G-042-X" → "G-042-X"
 */
export const getShortTokenId = (fullTokenId) => {
  if (!fullTokenId) return '';

  const firstHyphenIndex = fullTokenId.indexOf('-');

  // If no hyphen is found, return the original string
  if (firstHyphenIndex === -1) return fullTokenId;

  // Return everything AFTER the first hyphen
  return fullTokenId.slice(firstHyphenIndex + 1);
};