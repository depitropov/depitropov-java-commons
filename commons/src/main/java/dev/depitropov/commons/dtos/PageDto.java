package dev.depitropov.commons.dtos;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode
@Builder(toBuilder = true)
public class PageDto<T> {

  private int totalPages;

  private long totalElements;

  /** Page number starts from 1 */
  private int page;

  private int size;

  private List<T> content;

  private boolean first;

  private boolean last;

  private boolean hasPrevious;

  private boolean hasNext;

  public <S> PageDto<S> map(final Function<? super T, ? extends S> converter) {

    final List<S> newContent =
        isNotEmpty(getContent())
            ? getContent().stream().map(converter).collect(Collectors.toList())
            : List.of();

    return new PageDto<>(
        getTotalPages(),
        getTotalElements(),
        getPage(),
        getSize(),
        newContent,
        isFirst(),
        isLast(),
        isHasPrevious(),
        isHasNext());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("totalPages", totalPages)
        .append("totalElements", totalElements)
        .append("page", page)
        .append("size", size)
        .append("content.size", content.size())
        .append("first", first)
        .append("last", last)
        .append("hasPrevious", hasPrevious)
        .append("hasNext", hasNext)
        .toString();
  }
}
