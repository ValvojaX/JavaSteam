package com.javasteam.models.containers;

import com.javasteam.models.StructLoader;
import com.javasteam.models.loaders.MessageStructLoader;
import com.javasteam.models.loaders.ProtoMessageStructLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * StructContainer is a container class that holds the struct classes. It is used to group the
 * struct classes together. {@link StructLoader} instances can be registered with the container to
 * load the struct classes.
 */
public class StructContainer {
  private static final List<StructLoader<Object>> structLoaders = createInitialStructLoaders();

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<StructLoader<Object>> createInitialStructLoaders() {
    List<StructLoader<Object>> structLoaders = new ArrayList<>();

    for (var value : MessageStructLoader.values()) {
      structLoaders.add((StructLoader) value);
    }

    for (var value : ProtoMessageStructLoader.values()) {
      structLoaders.add((StructLoader) value);
    }

    return structLoaders;
  }

  @SuppressWarnings("unchecked")
  public static <D, T extends StructLoader<D>> void register(T... structLoaderArray) {
    structLoaders.addAll(List.of((StructLoader<Object>[]) structLoaderArray));
  }

  public static <D, T extends StructLoader<D>> void register(T[]... structLoaderArray) {
    for (T[] structLoader : structLoaderArray) {
      register(structLoader);
    }
  }

  @SuppressWarnings("unchecked")
  public static <D, T extends StructLoader<D>> Optional<T> getStructLoader(int emsg) {
    return structLoaders.stream()
        .filter(loader -> emsg == loader.getEmsg())
        .map(loader -> (T) loader)
        .findFirst();
  }
}
