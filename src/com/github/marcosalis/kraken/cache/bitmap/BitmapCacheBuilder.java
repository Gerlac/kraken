/*
 * Copyright 2013 Marco Salis - fast3r@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.marcosalis.kraken.cache.bitmap;

import java.io.IOException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import android.content.Context;

import com.github.marcosalis.kraken.cache.DiskCache;
import com.github.marcosalis.kraken.cache.DiskCache.DiskCacheClearMode;
import com.github.marcosalis.kraken.utils.DroidUtils;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;

/**
 * Public builder to customize and initialize a {@link BitmapCache}.
 * 
 * <b>Example usage:</b> TODO
 * 
 * @since 1.0
 * @author Marco Salis
 */
@Beta
@NotThreadSafe
public class BitmapCacheBuilder {

	final Context context;

	// first level cache config
	int memoryCacheMaxBytes;
	String memoryCacheLogName = "BitmapCache";

	// disk cache config
	String diskCacheDirectory;
	long purgeableAfterSeconds;

	public BitmapCacheBuilder(@Nonnull Context context) {
		this.context = context;
	}

	/**
	 * Sets the memory cache occupation limit in bytes. Use of
	 * {@link #maxMemoryCachePercentage(float)} is recommended as it enforces a
	 * proportional limit on the application memory class.
	 * 
	 * @param maxBytes
	 * @return This builder
	 */
	@Nonnull
	public BitmapCacheBuilder maxMemoryCacheBytes(@Nonnegative int maxBytes) {
		Preconditions.checkArgument(maxBytes > 0);
		memoryCacheMaxBytes = maxBytes;
		return this;
	}

	/**
	 * Sets the maximum percentage of application memory that the built cache
	 * can use.
	 * 
	 * Defaults to {@link BitmapLruCache#DEFAULT_MAX_MEMORY_PERCENTAGE}.
	 * Percentages above 30% are not recommended for a typical application.
	 * 
	 * @param percentage
	 *            A percentage value (0 < percentage <= 100)
	 * @return This builder
	 */
	@Nonnull
	public BitmapCacheBuilder maxMemoryCachePercentage(@Nonnegative float percentage) {
		Preconditions.checkArgument(percentage > 0f && percentage <= 100f);
		final int appMemoryClass = DroidUtils.getApplicationMemoryClass(context);
		memoryCacheMaxBytes = (int) ((appMemoryClass / 100f) * percentage); // %
		return this;
	}

	/**
	 * Sets an optional cache logging name. Only useful for debugging and cache
	 * statistics.
	 * 
	 * @param cacheName
	 * @return This builder
	 */
	@Nonnull
	public BitmapCacheBuilder memoryCacheLogName(@Nonnull String cacheName) {
		memoryCacheLogName = cacheName;
		return this;
	}

	/**
	 * Sets the <b>mandatory</b> disk cache directory name. Must be a valid
	 * Linux file name. Keep this consistent at each cache instantiation or the
	 * disk cache will be lost.
	 * 
	 * @param cacheDirectory
	 *            The cache directory (with no trailing slashes)
	 * @return This builder
	 */
	@Nonnull
	public BitmapCacheBuilder diskCacheDirectoryName(@Nonnull String cacheDirectory) {
		diskCacheDirectory = cacheDirectory;
		return this;
	}

	/**
	 * Sets the disk cache items expiration time in seconds. This is just a
	 * hint: it's not guaranteed that the actual implementation of the disk
	 * cache will automatically perform cache eviction at all. Call
	 * {@link BitmapCache#clearDiskCache(DiskCacheClearMode)} to manually purge
	 * old bitmaps from disk.
	 * 
	 * Defaults to {@link BitmapDiskCache#DEFAULT_PURGE_AFTER} if not set.
	 * 
	 * @param seconds
	 *            The cache items expiration. Must be >=
	 *            {@link DiskCache#MIN_EXPIRE_IN_SEC}
	 * @return This builder
	 */
	@Nonnull
	public BitmapCacheBuilder diskCachePurgeableAfter(long seconds) {
		Preconditions.checkArgument(seconds >= DiskCache.MIN_EXPIRE_IN_SEC);
		purgeableAfterSeconds = seconds;
		return this;
	}

	/**
	 * Builds the configured bitmap cache.
	 * 
	 * @return The built {@link BitmapCache} instance
	 * @throws IOException
	 *             If the disk cache creation failed
	 * @throws IllegalArgumentException
	 *             If one of the mandatory configuration parameters were not set
	 */
	@Nonnull
	public BitmapCache build() throws IOException {
		checkMandatoryValuesConsistency();
		setConfigDefaults();

		final BitmapLruCache<String> lruCache = buildLruCache();
		final BitmapDiskCache diskCache = buildDiskCache();
		return new BitmapCacheImpl(lruCache, diskCache);
	}

	private void checkMandatoryValuesConsistency() {
		Preconditions.checkNotNull(diskCacheDirectory, "Disk cache directory not set");
	}

	private void setConfigDefaults() {
		if (memoryCacheMaxBytes == 0) {
			maxMemoryCachePercentage(BitmapLruCache.DEFAULT_MAX_MEMORY_PERCENTAGE);
		}
		if (purgeableAfterSeconds == 0) {
			diskCachePurgeableAfter(BitmapDiskCache.DEFAULT_PURGE_AFTER);
		}
	}

	private BitmapLruCache<String> buildLruCache() {
		return new BitmapLruCache<String>(memoryCacheMaxBytes, memoryCacheLogName);
	}

	private BitmapDiskCache buildDiskCache() throws IOException {
		return new BitmapDiskCache(context, diskCacheDirectory, purgeableAfterSeconds);
	}

}