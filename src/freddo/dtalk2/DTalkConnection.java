/*
 * Copyright (c) 2013-2015 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package freddo.dtalk2;

import java.util.concurrent.Future;

/**
 * A construct that provides the means to send an ordered, lossless, stream of
 * bytes in both directions.
 * 
 * @author ggeorg
 */
public interface DTalkConnection {

	String getName();
	
	void setName(String name);

	Future<Void> sendMessage(String message);

	void close();

}
