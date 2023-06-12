/*
 * Copyright 2004-2019 the original author or authors.
 *
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
 */

package dev.macula.boot.starter.jpa.jpa.templatequery.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.BlobType;
import org.hibernate.type.descriptor.java.DataHelper;
import org.springframework.beans.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.convert.JodaTimeConverters;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * .
 * <p/>
 *
 * @author <a href="mailto:stormning@163.com">stormning</a>
 * @version V1.0, 15/11/24.
 */
public class BeanTransformerAdapter<T> implements ResultTransformer {

    private static final long serialVersionUID = -7081423491986799204L;

    private static DefaultConversionService conversionService;

    static {
        BeanTransformerAdapter.conversionService = new DefaultConversionService();
        Collection<Converter<?, ?>> convertersToRegister = JodaTimeConverters.getConvertersToRegister();

        for (Converter<?, ?> converter : convertersToRegister) {
            BeanTransformerAdapter.conversionService.addConverter(converter);
        }
        BeanTransformerAdapter.conversionService.addConverter(ClobToStringConverter.INSTANCE);
        BeanTransformerAdapter.conversionService.addConverter(BlobToStringConverter.INSTANCE);
    }

    /**
     * Logger available to subclasses
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * The class we are mapping to
     */
    private Class<T> mappedClass;

    /**
     * Whether we're strictly validating
     */
    private boolean checkFullyPopulated = false;

    /**
     * Whether we're defaulting primitives when mapping a null value
     */
    private boolean primitivesDefaultedForNullValue = false;

    /**
     * Map of the fields we provide mapping for
     */
    private Map<String, PropertyDescriptor> mappedFields;

    /**
     * Set of bean properties we provide mapping for
     */
    private Set<String> mappedProperties;

    /**
     * Create a new BeanPropertyRowMapper, accepting unpopulated properties in the target bean.
     * <p>Consider using the {@link #newInstance} factory method instead,
     * which allows for specifying the mapped type once only.
     *
     * @param mappedClass the class that each row should be mapped to
     */
    public BeanTransformerAdapter(Class<T> mappedClass) {
        initialize(mappedClass);
    }

    /**
     * Create a new BeanPropertyRowMapper.
     *
     * @param mappedClass         the class that each row should be mapped to
     * @param checkFullyPopulated whether we're strictly validating that all bean properties have been mapped from
     *                            corresponding database fields
     */
    public BeanTransformerAdapter(Class<T> mappedClass, boolean checkFullyPopulated) {
        initialize(mappedClass);
        this.checkFullyPopulated = checkFullyPopulated;
    }

    /**
     * Static factory method to create a new BeanPropertyRowMapper (with the mapped class specified only once).
     *
     * @param mappedClass the class that each row should be mapped to
     * @param <T>         class type
     * @return instance
     */
    public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
        BeanPropertyRowMapper<T> newInstance = new BeanPropertyRowMapper<T>();
        newInstance.setMappedClass(mappedClass);
        return newInstance;
    }

    /**
     * Initialize the mapping metadata for the given class.
     *
     * @param mappedClass the mapped class.
     */
    protected void initialize(Class<T> mappedClass) {
        this.mappedClass = mappedClass;
        this.mappedFields = new HashMap<String, PropertyDescriptor>();
        this.mappedProperties = new HashSet<String>();
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);
        for (PropertyDescriptor pd : pds) {
            this.mappedFields.put(pd.getName().toLowerCase(), pd);
            String underscoredName = underscoreName(pd.getName());
            if (!pd.getName().toLowerCase().equals(underscoredName)) {
                this.mappedFields.put(underscoredName, pd);
            }
            this.mappedProperties.add(pd.getName());
        }
    }

    /**
     * Convert a name in camelCase to an underscored name in lower case. Any upper case letters are converted to lower
     * case with a preceding underscore.
     *
     * @param name the string containing original name
     * @return the converted name
     */
    private String underscoreName(String name) {
        if (!StringUtils.hasLength(name)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(name.substring(0, 1).toLowerCase());
        for (int i = 1; i < name.length(); i++) {
            String s = name.substring(i, i + 1);
            String slc = s.toLowerCase();
            if (!s.equals(slc)) {
                result.append("_").append(slc);
            } else {
                result.append(s);
            }
        }
        return result.toString();
    }

    /**
     * Get the class that we are mapping to.
     *
     * @return mapped class
     */
    public final Class<T> getMappedClass() {
        return this.mappedClass;
    }

    /**
     * Set the class that each row should be mapped to.
     *
     * @param mappedClass mapped class
     */
    public void setMappedClass(Class<T> mappedClass) {
        if (this.mappedClass == null) {
            initialize(mappedClass);
        } else {
            if (!this.mappedClass.equals(mappedClass)) {
                throw new InvalidDataAccessApiUsageException(
                    "The mapped class can not be reassigned to map to " + mappedClass + " since it is already providing mapping for " + this.mappedClass);
            }
        }
    }

    /**
     * Return whether we're strictly validating that all bean properties have been mapped from corresponding database
     * fields.
     *
     * @return flag
     */
    public boolean isCheckFullyPopulated() {
        return this.checkFullyPopulated;
    }

    /**
     * Set whether we're strictly validating that all bean properties have been mapped from corresponding database
     * fields.
     * <p>Default is {@code false}, accepting unpopulated properties in the
     * target bean.
     */
    public void setCheckFullyPopulated(boolean checkFullyPopulated) {
        this.checkFullyPopulated = checkFullyPopulated;
    }

    /**
     * Return whether we're defaulting Java primitives in the case of mapping a null value from corresponding database
     * fields.
     */
    public boolean isPrimitivesDefaultedForNullValue() {
        return primitivesDefaultedForNullValue;
    }

    /**
     * Set whether we're defaulting Java primitives in the case of mapping a null value from corresponding database
     * fields.
     * <p>Default is {@code false}, throwing an exception when nulls are mapped to Java primitives.
     */
    public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
        this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
    }

    /**
     * Initialize the given BeanWrapper to be used for row mapping. To be called for each row.
     * <p>The default implementation is empty. Can be overridden in subclasses.
     *
     * @param bw the BeanWrapper to initialize
     */
    protected void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(conversionService);
    }

    /**
     * Retrieve a JDBC object value for the specified column.
     * <p>The default implementation calls
     * {@link JdbcUtils#getResultSetValue(ResultSet, int, Class)}. Subclasses may override this to check specific value
     * types upfront, or to post-process values return from {@code getResultSetValue}.
     *
     * @param rs    is the ResultSet holding the data
     * @param index is the column index
     * @param pd    the bean property that each result object is expected to match (or {@code null} if none specified)
     * @return the Object value
     * @throws SQLException in case of extraction failure
     * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(ResultSet, int, Class)
     */
    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
    }

    @Override
    @SuppressWarnings("deprecation")
    public Object transformTuple(Object[] tuple, String[] aliases) {
        T mappedObject = BeanUtils.instantiate(this.mappedClass);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
        initBeanWrapper(bw);

        Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<String>() : null);
        for (int i = 0; i < aliases.length; i++) {
            String column = aliases[i];
            PropertyDescriptor pd = this.mappedFields.get(column.replaceAll(" ", "").toLowerCase());
            if (pd != null) {
                try {
                    Object value = tuple[i];
                    try {
                        bw.setPropertyValue(pd.getName(), value);
                    } catch (TypeMismatchException e) {
                        if (value == null && primitivesDefaultedForNullValue) {
                            logger.debug(
                                "Intercepted TypeMismatchException for column " + column + " and column '" + column + "' with value " + value + " when setting property '" + pd.getName() + "' of type " + pd.getPropertyType() + " on object: " + mappedObject);
                        } else {
                            throw e;
                        }
                    }
                    if (populatedProperties != null) {
                        populatedProperties.add(pd.getName());
                    }
                } catch (NotWritablePropertyException ex) {
                    throw new DataRetrievalFailureException(
                        "Unable to map column " + column + " to property " + pd.getName(), ex);
                }
            }
        }

        if (populatedProperties != null && !populatedProperties.equals(this.mappedProperties)) {
            throw new InvalidDataAccessApiUsageException(
                "Given ResultSet does not contain all fields " + "necessary to populate object of class [" + this.mappedClass + "]: " + this.mappedProperties);
        }

        return mappedObject;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List transformList(List list) {
        return list;
    }

    enum ClobToStringConverter implements Converter<Clob, String> {

        INSTANCE;

        @Override
        public String convert(Clob source) {
            return DataHelper.extractString(source);
        }
    }

    enum BlobToStringConverter implements Converter<Blob, String> {

        INSTANCE;

        @Override
        public String convert(Blob source) {
            return BlobType.INSTANCE.toString(source);
        }
    }
}
