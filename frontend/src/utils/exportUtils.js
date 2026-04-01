import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

/**
 * Exports data to a CSV file and triggers a download.
 * @param {Array} data - Array of objects or arrays representing rows.
 * @param {Array} headers - Array of strings for the first row.
 * @param {string} filename - Name of the file to save.
 */
export const downloadCSV = (data, headers, filename) => {
  const csvContent = [
    headers.join(','),
    ...data.map(row => 
      row.map(cell => {
        const cellStr = String(cell || '').replace(/"/g, '""');
        return `"${cellStr}"`;
      }).join(',')
    )
  ].join('\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `${filename}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * Exports data to a PDF file and triggers a download.
 * @param {string} title - Title of the report.
 * @param {Array} headers - Array of strings for the table header.
 * @param {Array} rows - Array of arrays for the table body.
 * @param {string} filename - Name of the file to save.
 */
export const downloadPDF = (title, headers, rows, filename) => {
  const doc = new jsPDF();
  
  // Header
  doc.setFontSize(20);
  doc.setTextColor(11, 22, 41); // brand-dark color approximation
  doc.text(title, 14, 20);
  
  doc.setFontSize(10);
  doc.setTextColor(100);
  doc.text(`Generated on: ${new Date().toLocaleString()}`, 14, 28);
  
  autoTable(doc, {
    head: [headers],
    body: rows,
    startY: 35,
    styles: { fontSize: 8, cellPadding: 3 },
    headStyles: { fillColor: [61, 90, 254], textColor: 255 }, // brand-accent
    alternateRowStyles: { fillColor: [250, 250, 250] },
    margin: { top: 35 }
  });

  doc.save(`${filename}.pdf`);
};
