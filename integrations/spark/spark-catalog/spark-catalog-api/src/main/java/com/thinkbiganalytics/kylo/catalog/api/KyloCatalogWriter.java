package com.thinkbiganalytics.kylo.catalog.api;

/*-
 * #%L
 * Kylo Catalog API
 * %%
 * Copyright (C) 2017 - 2018 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.apache.spark.SparkContext;
import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.SaveMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Saves a Spark {@code DataFrame} to an external system.
 *
 * <p>Hadoop configuration properties can be set with {@code option()} by prefixing the key with "{@code spark.hadoop.}".</p>
 *
 * @param <T> Spark {@code DataFrame} class
 * @see KyloCatalogClient#write(Object)
 */
@SuppressWarnings("unused")
public interface KyloCatalogWriter<T> {

    /**
     * Adds a file to be downloaded with all Spark jobs.
     *
     * <p>NOTE: Local files cannot be used when Spark is running in yarn-cluster mode.</p>
     *
     * @param path can be either a local file, a file in HDFS (or other Hadoop-supported filesystem), or an HTTP/HTTPS/FTP URI
     * @see SparkContext#addFile(String)
     */
    @Nonnull
    KyloCatalogWriter<T> addFile(@Nullable String path);

    /**
     * Adds files to be downloaded with all Spark jobs.
     *
     * @see #addFile(String)
     */
    @Nonnull
    KyloCatalogWriter<T> addFiles(@Nullable java.util.List<String> paths);

    /**
     * (Scala-specific) Adds files to be downloaded with all Spark jobs.
     *
     * @see #addFile(String)
     */
    @Nonnull
    KyloCatalogWriter<T> addFiles(@Nullable scala.collection.Seq<String> paths);

    /**
     * Adds a JAR dependency containing the data source classes or its dependencies.
     *
     * <p>NOTE: Local jars cannot be used when Spark is running in yarn-cluster mode.</p>
     *
     * @param path can be either a local file, a file in HDFS (or other Hadoop-supported filesystem), an HTTP/HTTPS/FTP URI, or local:/path (for a file on every worker node)
     * @see SparkContext#addJar(String)
     */
    @Nonnull
    KyloCatalogWriter<T> addJar(@Nullable String path);

    /**
     * Adds JAR dependencies containing the data source classes and its dependencies.
     *
     * @see #addJar(String)
     */
    @Nonnull
    KyloCatalogWriter<T> addJars(@Nullable java.util.List<String> paths);

    /**
     * (Scala-specific) Adds JAR dependencies containing the data source classes and its dependencies.
     *
     * @see #addJar(String)
     */
    @Nonnull
    KyloCatalogWriter<T> addJars(@Nullable scala.collection.Seq<String> paths);

    /**
     * Buckets the output by the given columns. If specified, the output is laid out on the file system similar to Hive's bucketing scheme.
     *
     * <p>Requires Spark 2.0+</p>
     *
     * @see "org.apache.spark.sql.DataFrameWriter#bucketBy(int, String, String...)"
     */
    @Nonnull
    KyloCatalogWriter<T> bucketBy(int numBuckets, @Nonnull String colName, String... colNames);

    /**
     * Specifies the underlying output data source.
     *
     * @see DataFrameWriter#format(String)
     */
    @Nonnull
    KyloCatalogWriter<T> format(@Nonnull String source);

    /**
     * Specifies the behavior when data or table already exists.
     *
     * @see DataFrameWriter#mode(String)
     */
    @Nonnull
    KyloCatalogWriter<T> mode(@Nonnull String saveMode);

    /**
     * Specifies the behavior when data or table already exists.
     *
     * @see DataFrameWriter#mode(String)
     */
    @Nonnull
    KyloCatalogWriter<T> mode(@Nonnull SaveMode saveMode);

    /**
     * Adds an output option for the underlying data source.
     *
     * @see "org.apache.spark.sql.DataFrameWriter#option(String, double)"
     */
    @Nonnull
    KyloCatalogWriter<T> option(@Nonnull String key, double value);

    /**
     * Adds an output option for the underlying data source.
     *
     * @see "org.apache.spark.sql.DataFrameWriter#option(String, long)"
     */
    @Nonnull
    KyloCatalogWriter<T> option(@Nonnull String key, long value);

    /**
     * Adds an output option for the underlying data source.
     *
     * @see "org.apache.spark.sql.DataFrameWriter#option(String, boolean)"
     */
    @Nonnull
    KyloCatalogWriter<T> option(@Nonnull String key, boolean value);

    /**
     * Adds an output option for the underlying data source.
     *
     * @see DataFrameWriter#option(String, String)
     */
    @Nonnull
    KyloCatalogWriter<T> option(@Nonnull String key, @Nullable String value);

    /**
     * Adds output options for the underlying data source.
     *
     * @see DataFrameWriter#options(java.util.Map)
     */
    @Nonnull
    KyloCatalogWriter<T> options(@Nullable java.util.Map<String, String> options);

    /**
     * (Scala-specific) Adds output options for the underlying data source.
     *
     * @see DataFrameWriter#options(scala.collection.Map)
     */
    @Nonnull
    KyloCatalogWriter<T> options(@Nullable scala.collection.Map<String, String> options);

    /**
     * Partitions the output by the given columns on the file system. If specified, the output is laid out on the file system similar to Hive's partitioning scheme.
     *
     * @see DataFrameWriter#partitionBy(String...)
     */
    @Nonnull
    KyloCatalogWriter<T> partitionBy(@Nonnull String... colNames);

    /**
     * Saves the content of the {@code DataFrame} as the specified table.
     *
     * @throws KyloCatalogException if the data cannot be saved
     * @see DataFrameWriter#save()
     */
    void save();

    /**
     * Saves the content of the {@code DataFrame} at the specified path.
     *
     * @throws KyloCatalogException if the data cannot be saved
     * @see DataFrameWriter#save(String)
     */
    void save(@Nonnull String path);

    /**
     * Sorts the output in each bucket by the given columns.
     *
     * <p>Requires Spark 2.0+</p>
     *
     * @see "DataFrameWriter#org.apache.spark.sql.DataFrameWriter(String, String...)"
     */
    @Nonnull
    KyloCatalogWriter<T> sortBy(@Nonnull String colName, String... colNames);
}
