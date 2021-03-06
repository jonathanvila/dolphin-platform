/*
 * Copyright 2015-2017 Canoo Engineering AG.
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
package com.canoo.dolphin.converters;

import com.canoo.platform.remoting.spi.converter.Converter;
import com.canoo.platform.remoting.spi.converter.ValueConverterException;
import com.canoo.dp.impl.remoting.Converters;
import com.canoo.dp.impl.remoting.BeanRepository;
import mockit.Mocked;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class DurationConverterFactoryTest {

    @Test
    public void testFactoryFieldType(@Mocked BeanRepository beanRepository) {
        //Given
        Converters converters = new Converters(beanRepository);

        //When
        int type = converters.getFieldType(Duration.class);

        //Then
        assertEquals(type, ValueFieldTypes.DURATION_FIELD_TYPE);
    }

    @Test
    public void testConverterCreation(@Mocked BeanRepository beanRepository) {
        //Given
        Converters converters = new Converters(beanRepository);

        //When
        Converter converter = converters.getConverter(Duration.class);

        //Then
        assertNotNull(converter);
    }

    @Test
    public void testBasicConversions(@Mocked BeanRepository beanRepository) {
        //Given
        Converters converters = new Converters(beanRepository);

        //When
        Converter converter = converters.getConverter(Duration.class);

        //Then
        testReconversion(converter, Duration.ZERO);
        testReconversion(converter, Duration.ofDays(7));
        testReconversion(converter, Duration.ofMillis(10));
        testReconversion(converter, Duration.ofDays(10_000_000));
    }

    @Test
    public void testNullValues(@Mocked BeanRepository beanRepository) {
        //Given
        Converters converters = new Converters(beanRepository);

        //When
        Converter converter = converters.getConverter(Duration.class);

        //Then
        try {
            assertEquals(converter.convertFromDolphin(null), null);
            assertEquals(converter.convertToDolphin(null), null);
        } catch (ValueConverterException e) {
            fail("Error in conversion");
        }
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testWrongDolphinValues(@Mocked BeanRepository beanRepository) throws ValueConverterException{
        //Given
        Converters converters = new Converters(beanRepository);

        //When
        Converter converter = converters.getConverter(Duration.class);

        //Then
        converter.convertFromDolphin(7);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testWrongBeanValues(@Mocked BeanRepository beanRepository) throws ValueConverterException{
        //Given
        Converters converters = new Converters(beanRepository);

        //When
        Converter converter = converters.getConverter(Duration.class);

        //Then
        converter.convertToDolphin(7);
    }

    private void testReconversion(Converter converter, Duration duration) {
        try {
            Object dolphinObject = converter.convertToDolphin(duration);
            assertNotNull(dolphinObject);
            Object reconvertedObject = converter.convertFromDolphin(dolphinObject);
            assertNotNull(reconvertedObject);
            assertEquals(reconvertedObject.getClass(), Duration.class);
            Duration reconvertedDuration = (Duration) reconvertedObject;
            assertEquals(reconvertedDuration, duration);
        } catch (ValueConverterException e) {
            fail("Error in conversion", e);
        }
    }
}
