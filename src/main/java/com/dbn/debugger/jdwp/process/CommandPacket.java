/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.debugger.jdwp.process;

import org.jetbrains.annotations.NonNls;

public class CommandPacket extends Packet {

	public CommandPacket() {
		super();
	}

	public byte getCommandSet() {
		return getErrorCommand1();
	}

	public byte getCommand() {
		return getErrorCommand2();
	}

	public void setCommandSet(byte commandSet) {
		setErrorCommand1(commandSet);
	}

	public void setCommand(byte command2) {
		setErrorCommand2(command2);
	}


	public String toString() {
		@NonNls
		StringBuilder builder = new StringBuilder(super.toString());
		
		builder.append(", commandset: ");
		builder.append(getCommandSet());
		builder.append(", command: ");
		builder.append(getCommand());
		builder.append(", payload size: ");
		builder.append(getData().length);
		return builder.toString();
	}
}
