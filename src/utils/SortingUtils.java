package utils;

import model.LibraryItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hand-coded sorting algorithms for the library catalogue.
 * No use of Collections.sort() or Arrays.sort() — all implemented from scratch.
 *
 * Supported fields:     TITLE | AUTHOR | YEAR
 * Supported algorithms: SELECTION | INSERTION | MERGE | QUICK
 */
public class SortingUtils {

    public enum SortField     { TITLE, AUTHOR, YEAR }
    public enum SortAlgorithm { SELECTION, INSERTION, MERGE, QUICK }

    // ── Public entry point ───────────────────────────────────────────────────

    /**
     * Sorts a copy of the supplied list and returns the sorted copy.
     * The original list is not modified.
     */
    public List<LibraryItem> sort(List<LibraryItem> items,
                                  SortField field,
                                  SortAlgorithm algorithm) {
        List<LibraryItem> copy = new ArrayList<>(items);
        Comparator<LibraryItem> cmp = comparatorFor(field);

        switch (algorithm) {
            case SELECTION: selectionSort(copy, cmp); break;
            case INSERTION: insertionSort(copy, cmp); break;
            case MERGE:     mergeSort(copy, 0, copy.size() - 1, cmp); break;
            case QUICK:     quickSort(copy, 0, copy.size() - 1, cmp); break;
        }
        return copy;
    }

    // ── Comparators ──────────────────────────────────────────────────────────

    private Comparator<LibraryItem> comparatorFor(SortField field) {
        switch (field) {
            case TITLE:  return Comparator.comparing(i -> i.getTitle().toLowerCase());
            case AUTHOR: return Comparator.comparing(i -> i.getAuthor().toLowerCase());
            case YEAR:   return Comparator.comparingInt(LibraryItem::getYear);
            default:     return Comparator.comparing(i -> i.getTitle().toLowerCase());
        }
    }

    // ── Selection Sort  O(n²) ────────────────────────────────────────────────

    private void selectionSort(List<LibraryItem> list, Comparator<LibraryItem> cmp) {
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                if (cmp.compare(list.get(j), list.get(minIdx)) < 0) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                LibraryItem tmp = list.get(i);
                list.set(i, list.get(minIdx));
                list.set(minIdx, tmp);
            }
        }
    }

    // ── Insertion Sort  O(n²) best O(n) ─────────────────────────────────────

    private void insertionSort(List<LibraryItem> list, Comparator<LibraryItem> cmp) {
        int n = list.size();
        for (int i = 1; i < n; i++) {
            LibraryItem key = list.get(i);
            int j = i - 1;
            while (j >= 0 && cmp.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    // ── Merge Sort  O(n log n) — RECURSIVE ──────────────────────────────────

    private void mergeSort(List<LibraryItem> list, int lo, int hi,
                           Comparator<LibraryItem> cmp) {
        if (lo >= hi) return;                          // base case
        int mid = (lo + hi) / 2;
        mergeSort(list, lo, mid, cmp);                 // sort left half
        mergeSort(list, mid + 1, hi, cmp);             // sort right half
        merge(list, lo, mid, hi, cmp);                 // merge results
    }

    private void merge(List<LibraryItem> list, int lo, int mid, int hi,
                       Comparator<LibraryItem> cmp) {
        // Copy into temporary sublists
        List<LibraryItem> left  = new ArrayList<>(list.subList(lo, mid + 1));
        List<LibraryItem> right = new ArrayList<>(list.subList(mid + 1, hi + 1));

        int i = 0, j = 0, k = lo;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                list.set(k++, left.get(i++));
            } else {
                list.set(k++, right.get(j++));
            }
        }
        while (i < left.size())  list.set(k++, left.get(i++));
        while (j < right.size()) list.set(k++, right.get(j++));
    }

    // ── Quick Sort  O(n log n) avg — RECURSIVE ───────────────────────────────

    private void quickSort(List<LibraryItem> list, int lo, int hi,
                           Comparator<LibraryItem> cmp) {
        if (lo >= hi) return;                          // base case
        int pivotIdx = partition(list, lo, hi, cmp);
        quickSort(list, lo, pivotIdx - 1, cmp);        // sort left of pivot
        quickSort(list, pivotIdx + 1, hi, cmp);        // sort right of pivot
    }

    private int partition(List<LibraryItem> list, int lo, int hi,
                          Comparator<LibraryItem> cmp) {
        LibraryItem pivot = list.get(hi);
        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                LibraryItem tmp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, tmp);
            }
        }
        LibraryItem tmp = list.get(i + 1);
        list.set(i + 1, list.get(hi));
        list.set(hi, tmp);
        return i + 1;
    }
}
