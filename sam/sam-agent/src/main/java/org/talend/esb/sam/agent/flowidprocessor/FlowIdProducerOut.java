/*
 * #%L
 * Service Activity Monitoring :: Agent
 * %%
 * Copyright (C) 2011 Talend Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.talend.esb.sam.agent.flowidprocessor;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.talend.esb.sam.agent.flowid.FlowIdHelper;

public class FlowIdProducerOut<T extends Message> extends
		AbstractPhaseInterceptor<T> {

	protected static Logger logger = Logger.getLogger(FlowIdProducerOut.class
			.getName());

	public FlowIdProducerOut() {
		super(Phase.USER_LOGICAL);
	}

	public void handleMessage(T message) throws Fault {
		logger.finest("FlowIdProducerOut Interceptor called. isOutbound: "
				+ MessageUtils.isOutbound(message) + ", isRequestor: "
				+ MessageUtils.isRequestor(message));

		if (MessageUtils.isRequestor(message)) {
			handleRequestOut(message);
		} else {
			handleResponseOut(message);
		}
		
		String flowId = FlowIdHelper.getFlowId(message);
		FlowIdProtocolHeaderCodec.writeFlowId(message, flowId);
		FlowIdSoapCodec.writeFlowId(message, flowId);

	}

	protected void handleResponseOut(T message) throws Fault {
		logger.fine("handleResponseOut");

		Message reqMsg = message.getExchange().getInMessage();
		if (reqMsg == null) {
			logger.warning("getInMessage is null");
			return;
		}

		String reqFid = FlowIdHelper.getFlowId(reqMsg);
		FlowIdHelper.setFlowId(message, reqFid);

	}

	protected void handleRequestOut(T message) throws Fault {
		logger.fine("handleRequestIn");

		String flowId = FlowIdHelper.getFlowId(message);
		if (flowId == null && message.containsKey(PhaseInterceptorChain.PREVIOUS_MESSAGE)) {
			// Web Service consumer is acting as an intermediary
			logger.info("PREVIOUS_MESSAGE FOUND!!!");
			@SuppressWarnings("unchecked")
			WeakReference<Message> wrPreviousMessage = (WeakReference<Message>) message
					.get(PhaseInterceptorChain.PREVIOUS_MESSAGE);
			Message previousMessage = (Message) wrPreviousMessage.get();
			flowId = FlowIdHelper.getFlowId(previousMessage);
			if (flowId != null) {
				logger.fine("flowId '" + flowId + "' found in previous message");
			}

		}
		
		if (flowId == null) {
			// No flowId found. Generate one.
			logger.fine("Generate and add flowId");
			flowId = ContextUtils.generateUUID();
			FlowIdHelper.setFlowId(message, flowId);
			logger.info("FlowId '" + flowId + "' added to FlowId");
		}

		FlowIdHelper.setFlowId(message, flowId);
	}

}