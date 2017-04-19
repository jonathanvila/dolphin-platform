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
package org.opendolphin.core.client.comm

import org.junit.Assert
import org.opendolphin.LogConfig
import org.opendolphin.core.comm.Command
import org.opendolphin.core.comm.CreatePresentationModelCommand
import org.opendolphin.core.comm.GetPresentationModelCommand
import org.opendolphin.core.comm.ValueChangedCommand

import java.util.concurrent.TimeUnit
import java.util.logging.Level

class BlindCommandBatcherTest extends GroovyTestCase {

    private BlindCommandBatcher batcher;

    @Override
    protected void setUp() throws Exception {
        batcher = new BlindCommandBatcher();
        batcher.setDeferMillis(50);
    }

    void testMultipleBlindsAreBatchedNonMerging() {
        doMultipleBlindsAreBatched();
    }

    void testMultipleBlindsAreBatchedMerging() {
        batcher.setMergeValueChanges(true);
        doMultipleBlindsAreBatched();
    }

    void doMultipleBlindsAreBatched() {
        Assert.assertTrue(batcher.isEmpty());
        List<CommandAndHandler> list = Arrays.asList(new CommandAndHandler(null), new CommandAndHandler(null), new CommandAndHandler(null));
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }
        Assert.assertEquals(list, batcher.getWaitingBatches().getVal());
    }

    void testNonBlindForcesBatchNonMerging() {
        doNonBlindForcesBatch();
    }

    void testNonBlindForcesBatchMerging() {
        batcher.setMergeValueChanges(true);
        doNonBlindForcesBatch();
    }

    void doNonBlindForcesBatch() {
        Assert.assertTrue(batcher.isEmpty());

        List<CommandAndHandler> list = new ArrayList<>();
        list.add(new CommandAndHandler(null));
        list.add(new CommandAndHandler(null));
        list.add(new CommandAndHandler(null));
        list.add(new CommandAndHandler(null, new OnFinishedHandler() {

            @Override
            void onFinished() {

            }
        }));
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }
        Assert.assertEquals(4, list.size());
        Assert.assertEquals(list.subList(0, 3), batcher.getWaitingBatches().getVal());
        Assert.assertEquals(Collections.singletonList(list.get(3)), batcher.getWaitingBatches().getVal());
    }


    void testMaxBatchSizeNonMerging() {
        doMaxBatchSize();
    }

    void testMaxBatchSizeMerging() {
        batcher.setMergeValueChanges(true);
        doMaxBatchSize();
    }

    void doMaxBatchSize() {
        //given:
        batcher.setMaxBatchSize(4);
        ArrayList<CommandAndHandler> list = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            list.add(new CommandAndHandler(null));
        }

        //when:
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }

        //then:
        Assert.assertEquals(4, batcher.getWaitingBatches().getVal().size());
        Assert.assertEquals(4, batcher.getWaitingBatches().getVal().size());
        Assert.assertEquals(4, batcher.getWaitingBatches().getVal().size());
        Assert.assertEquals(4, batcher.getWaitingBatches().getVal().size());
        Assert.assertEquals(1, batcher.getWaitingBatches().getVal().size());
        Assert.assertTrue(batcher.isEmpty());
    }

    void testMergeInOneCommand() {

        //given:
        LogConfig.logOnLevel(Level.ALL);
        batcher.setMergeValueChanges(true);
        List<CommandAndHandler> list = new ArrayList<>();
        list.add(new CommandAndHandler(new ValueChangedCommand(attributeId: 0, oldValue: 0, newValue: 1)));
        list.add(new CommandAndHandler(new ValueChangedCommand(attributeId: 0, oldValue: 1, newValue: 2)));
        list.add(new CommandAndHandler(new ValueChangedCommand(attributeId: 0, oldValue: 2, newValue: 3)));

        //when:
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }

        //then:
        List<CommandAndHandler> nextBatch = batcher.getWaitingBatches().getVal();
        Assert.assertEquals(1, nextBatch.size());
        Assert.assertEquals(ValueChangedCommand.class, nextBatch.get(0).getCommand().class);
        ValueChangedCommand cmd = nextBatch.get(0).getCommand();
        Assert.assertEquals(0, cmd.getOldValue());
        Assert.assertEquals(3, cmd.getNewValue());
        Assert.assertTrue(batcher.isEmpty());
    }

    void testMergeCreatePmAfterValueChange() {

        //given:
        batcher.setMergeValueChanges(true);
        List<CommandAndHandler> list = new ArrayList<>();
        list.add(new CommandAndHandler(new ValueChangedCommand(attributeId: 0, oldValue: 0, newValue: 1)));
        list.add(new CommandAndHandler(new CreatePresentationModelCommand()));

        //when:
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }

        //then:
        List<CommandAndHandler> nextBatch = batcher.getWaitingBatches().getVal();
        Assert.assertEquals(2, nextBatch.size());
        Assert.assertEquals(ValueChangedCommand.class, nextBatch.get(0).getCommand().class);
        Assert.assertEquals(CreatePresentationModelCommand.class, nextBatch.get(1).getCommand().class);
        Assert.assertTrue(batcher.isEmpty());

    }

    void testDropMultipleGetPmCommands() {

        //given:
        Command cmd1 = new GetPresentationModelCommand(pmId: 1)
        Command cmd2 = new GetPresentationModelCommand(pmId: 1)
        OnFinishedHandler sameHandler = new OnFinishedHandler() {
            @Override
            void onFinished() {

            }
        };
        List<CommandAndHandler> list = new ArrayList<>();
        list.add(new CommandAndHandler(cmd1, sameHandler));
        list.add(new CommandAndHandler(cmd2, sameHandler));
        list.add(new CommandAndHandler(cmd2));


        //when:
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }

        //then:
        List<CommandAndHandler> nextBatch = batcher.getWaitingBatches().getVal();
        Assert.assertEquals(1, nextBatch.size());
        Assert.assertEquals(GetPresentationModelCommand.class, nextBatch.get(0).getCommand().class);
        Assert.assertTrue(batcher.isEmpty());
    }

    void testVeryManyGetPmCommands() {
        //given:
        ArrayList<CommandAndHandler> list = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            Command cmd1 = new GetPresentationModelCommand();
            cmd1.setPmId("" + i);
            Command cmd2 = new GetPresentationModelCommand();
            cmd2.setPmId("" + i);
            list.add(new CommandAndHandler(cmd1, null)); // will be batched
            list.add(new CommandAndHandler(cmd2, null)); // will be dropped
        }

        //when:
        for (CommandAndHandler commandAndHandler : list) {
            batcher.batch(commandAndHandler);
        }

        //then:
        def resultCount = 0
        List<CommandAndHandler> nextBatch = batcher.getWaitingBatches().getVal(1, TimeUnit.SECONDS);
        while (nextBatch != null && !nextBatch.isEmpty()) {
            resultCount += nextBatch.size();
            nextBatch = batcher.getWaitingBatches().getVal(100, TimeUnit.MILLISECONDS);

        }
        Assert.assertEquals(300, resultCount);
    }

}
