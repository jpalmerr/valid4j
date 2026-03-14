package io.github.jpalmerr.valid4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A non-empty list: always has at least one element. Mirrors Cats' {@code NonEmptyList} type.
 *
 * <p>Guaranteed never empty by construction. Useful as an error accumulation type: if you have a
 * {@code NonEmptyList<E>}, you know at least one error occurred.
 *
 * @param <A> element type
 */
public record NonEmptyList<A>(A head, List<A> tail) implements Iterable<A> {

  /** Compact constructor: validates head is non-null and defensively copies tail. */
  public NonEmptyList {
    Objects.requireNonNull(head, "head must not be null");
    Objects.requireNonNull(tail, "tail must not be null");
    tail = List.copyOf(tail);
  }

  /**
   * Creates a NonEmptyList from a head element and zero or more tail elements.
   *
   * @param head the first element (required, must not be null)
   * @param rest additional elements (must not contain nulls)
   * @param <A> element type
   * @return a new NonEmptyList
   */
  @SafeVarargs
  public static <A> NonEmptyList<A> of(A head, A... rest) {
    Objects.requireNonNull(head, "head must not be null");
    List<A> tail = new ArrayList<>(rest.length);
    for (A element : rest) {
      Objects.requireNonNull(element, "tail elements must not be null");
      tail.add(element);
    }
    return new NonEmptyList<>(head, tail);
  }

  /**
   * Tries to create a NonEmptyList from a standard list.
   *
   * @param list source list (must not be null)
   * @param <A> element type
   * @return {@code Optional.empty()} if the list is empty, otherwise a present NonEmptyList
   */
  public static <A> Optional<NonEmptyList<A>> fromList(List<A> list) {
    Objects.requireNonNull(list, "list must not be null");
    if (list.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new NonEmptyList<>(list.get(0), list.subList(1, list.size())));
  }

  /**
   * Returns all elements as an unmodifiable list, head first then tail.
   *
   * @return unmodifiable list containing all elements
   */
  public List<A> toList() {
    List<A> result = new ArrayList<>(1 + tail.size());
    result.add(head);
    result.addAll(tail);
    return Collections.unmodifiableList(result);
  }

  /**
   * Appends a single element to the end of this list.
   *
   * @param element element to append (must not be null)
   * @return a new NonEmptyList with the element at the end
   */
  public NonEmptyList<A> append(A element) {
    Objects.requireNonNull(element, "element must not be null");
    List<A> newTail = new ArrayList<>(tail.size() + 1);
    newTail.addAll(tail);
    newTail.add(element);
    return new NonEmptyList<>(head, newTail);
  }

  /**
   * Concatenates this list with another NonEmptyList.
   *
   * @param other the list to append (must not be null)
   * @return a new NonEmptyList containing all elements of both lists
   */
  public NonEmptyList<A> appendAll(NonEmptyList<A> other) {
    Objects.requireNonNull(other, "other must not be null");
    List<A> newTail = new ArrayList<>(tail.size() + 1 + other.tail.size());
    newTail.addAll(tail);
    newTail.add(other.head);
    newTail.addAll(other.tail);
    return new NonEmptyList<>(head, newTail);
  }

  /**
   * Transforms every element using the provided function.
   *
   * @param f mapping function (must not be null)
   * @param <B> result element type
   * @return a new NonEmptyList with all elements transformed
   */
  public <B> NonEmptyList<B> map(Function<? super A, ? extends B> f) {
    Objects.requireNonNull(f, "f must not be null");
    List<B> newTail = new ArrayList<>(tail.size());
    for (A element : tail) {
      newTail.add(f.apply(element));
    }
    return new NonEmptyList<>(f.apply(head), newTail);
  }

  /**
   * Returns the number of elements (always at least 1).
   *
   * @return 1 + tail size
   */
  public int size() {
    return 1 + tail.size();
  }

  /**
   * Returns an iterator over all elements, head first then tail in order.
   *
   * @return iterator over all elements
   */
  @Override
  public Iterator<A> iterator() {
    return new Iterator<>() {
      private boolean headReturned = false;
      private final Iterator<A> tailIterator = tail.iterator();

      @Override
      public boolean hasNext() {
        return !headReturned || tailIterator.hasNext();
      }

      @Override
      public A next() {
        if (!headReturned) {
          headReturned = true;
          return head;
        }
        if (!tailIterator.hasNext()) {
          throw new NoSuchElementException();
        }
        return tailIterator.next();
      }
    };
  }
}
