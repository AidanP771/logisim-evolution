/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fork.palette;

import com.cburch.logisim.fork.ForkPreferences;
import com.cburch.logisim.gui.search.SearchContext;
import com.cburch.logisim.gui.search.SearchProvider;
import com.cburch.logisim.gui.search.SearchQuery;
import com.cburch.logisim.gui.search.SearchResult;
import java.util.List;
import java.util.function.Predicate;

/**
 * Wraps a built-in search provider so it honours the palette's {@code >} / {@code @} prefixes and
 * ignores a trailing number. With the palette flag off it behaves exactly like the wrapped provider.
 */
final class ScopedProvider implements SearchProvider {
  private final SearchProvider delegate;
  private final Predicate<PaletteQuery.Scope> includedIn;

  ScopedProvider(SearchProvider delegate, Predicate<PaletteQuery.Scope> includedIn) {
    this.delegate = delegate;
    this.includedIn = includedIn;
  }

  SearchProvider getDelegate() {
    return delegate;
  }

  @Override
  public String getDisplayName() {
    return delegate.getDisplayName();
  }

  @Override
  public void prepare(SearchContext context) {
    delegate.prepare(context);
  }

  @Override
  public List<SearchResult> search(SearchQuery query) {
    if (!ForkPreferences.PALETTE.isEnabled()) return delegate.search(query);
    final var parsed = PaletteQuery.parse(query.text());
    if (!includedIn.test(parsed.scope())) return List.of();
    // An empty search after a prefix lists everything this provider offers.
    return delegate.search(new SearchQuery(parsed.text()));
  }

  @Override
  public boolean isAsynchronous() {
    return delegate.isAsynchronous();
  }

  @Override
  public int getPriority() {
    return delegate.getPriority();
  }

  @Override
  public boolean isAvailable(SearchContext context) {
    return delegate.isAvailable(context);
  }
}
