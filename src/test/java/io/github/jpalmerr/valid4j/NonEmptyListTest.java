package io.github.jpalmerr.valid4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NonEmptyListTest {

  @Test
  void of_singleElement_createsListWithEmptyTail() {
    NonEmptyList<String> nel = NonEmptyList.of("only");

    assertThat(nel.head()).isEqualTo("only");
    assertThat(nel.tail()).isEmpty();
  }

  @Test
  void of_multipleElements_createsListWithCorrectHeadAndTail() {
    NonEmptyList<Integer> nel = NonEmptyList.of(1, 2, 3);

    assertThat(nel.head()).isEqualTo(1);
    assertThat(nel.tail()).containsExactly(2, 3);
  }

  @Test
  void of_nullHead_throwsNullPointerException() {
    assertThatNullPointerException().isThrownBy(() -> NonEmptyList.of(null, "a", "b"));
  }

  @Test
  void fromList_nonEmptyList_returnsPresent() {
    Optional<NonEmptyList<String>> result = NonEmptyList.fromList(List.of("a", "b", "c"));

    assertThat(result).isPresent();
    assertThat(result.get().head()).isEqualTo("a");
    assertThat(result.get().tail()).containsExactly("b", "c");
  }

  @Test
  void fromList_emptyList_returnsEmpty() {
    Optional<NonEmptyList<String>> result = NonEmptyList.fromList(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void fromList_singletonList_returnsNelWithEmptyTail() {
    Optional<NonEmptyList<String>> result = NonEmptyList.fromList(List.of("only"));

    assertThat(result).isPresent();
    assertThat(result.get().head()).isEqualTo("only");
    assertThat(result.get().tail()).isEmpty();
  }

  @Test
  void toList_returnsUnmodifiableListOfAllElements() {
    NonEmptyList<String> nel = NonEmptyList.of("a", "b", "c");

    List<String> list = nel.toList();

    assertThat(list).containsExactly("a", "b", "c");
    assertThatThrownBy(() -> list.add("d")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void append_addsElementToEnd() {
    NonEmptyList<String> nel = NonEmptyList.of("a", "b");

    NonEmptyList<String> result = nel.append("c");

    assertThat(result.toList()).containsExactly("a", "b", "c");
  }

  @Test
  void appendAll_concatenatesTwoLists() {
    NonEmptyList<Integer> first = NonEmptyList.of(1, 2);
    NonEmptyList<Integer> second = NonEmptyList.of(3, 4, 5);

    NonEmptyList<Integer> result = first.appendAll(second);

    assertThat(result.toList()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  void map_transformsAllElements() {
    NonEmptyList<Integer> nel = NonEmptyList.of(1, 2, 3);

    NonEmptyList<String> result = nel.map(Object::toString);

    assertThat(result.toList()).containsExactly("1", "2", "3");
  }

  @Test
  void size_returnsCorrectCount() {
    assertThat(NonEmptyList.of("a").size()).isEqualTo(1);
    assertThat(NonEmptyList.of("a", "b", "c").size()).isEqualTo(3);
  }

  @Test
  void iterator_iteratesAllElementsInOrder() {
    NonEmptyList<String> nel = NonEmptyList.of("x", "y", "z");

    Iterator<String> it = nel.iterator();
    List<String> collected = new ArrayList<>();
    while (it.hasNext()) {
      collected.add(it.next());
    }

    assertThat(collected).containsExactly("x", "y", "z");
  }

  @Test
  void iterator_supportsForEachLoop() {
    NonEmptyList<Integer> nel = NonEmptyList.of(10, 20, 30);

    List<Integer> collected = new ArrayList<>();
    for (Integer element : nel) {
      collected.add(element);
    }

    assertThat(collected).containsExactly(10, 20, 30);
  }

  @Test
  void iterator_callingNextPastEnd_throwsNoSuchElementException() {
    NonEmptyList<String> nel = NonEmptyList.of("a");

    Iterator<String> it = nel.iterator();
    it.next();

    assertThatThrownBy(it::next).isInstanceOf(java.util.NoSuchElementException.class);
  }

  @Test
  void iterator_multipleIndependentIterators_doNotInterfere() {
    NonEmptyList<Integer> nel = NonEmptyList.of(1, 2, 3);

    Iterator<Integer> first = nel.iterator();
    Iterator<Integer> second = nel.iterator();

    first.next();

    List<Integer> fromSecond = new ArrayList<>();
    while (second.hasNext()) {
      fromSecond.add(second.next());
    }

    assertThat(first.next()).isEqualTo(2);
    assertThat(fromSecond).containsExactly(1, 2, 3);
  }

  @Test
  void iterator_singleElementNel_iteratesOnlyHead() {
    NonEmptyList<String> nel = NonEmptyList.of("only");

    Iterator<String> it = nel.iterator();

    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("only");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void iterator_remove_throwsUnsupportedOperationException() {
    Iterator<String> it = NonEmptyList.of("a").iterator();
    it.next();
    assertThatThrownBy(it::remove).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void equals_sameElements_areEqual() {
    NonEmptyList<String> first = NonEmptyList.of("a", "b");
    NonEmptyList<String> second = NonEmptyList.of("a", "b");

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  void toString_showsRecordFormat() {
    NonEmptyList<String> nel = NonEmptyList.of("a", "b");

    String str = nel.toString();

    assertThat(str).contains("NonEmptyList");
    assertThat(str).contains("a");
    assertThat(str).contains("b");
  }
}
