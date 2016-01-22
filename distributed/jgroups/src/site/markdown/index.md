##	Distributed support: JGroups provider

Uses a JGroups channel to broadcast cluster membership changes to other peer nodes.
The JGroups provider recognizes the following properties:
<table>
	<tr>
		<th>Property</th>
		<th>Default</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>**stack**</td>
		<td>`udp-sync.xml`</td>
		<td>
			Defines one of the following:
			<ul>
				<li>Name of a system resource containing the JGroups XML configuration.</li>
				<li>URL of the JGroups XML configuration file.</li>
				<li>Path of the JGroups XML configuration on the local file system.</li>
				<li>Legacy protocol stack property string.</li>
			</ul>
			See the [JGroups wiki][jgroups] for assistance with customizing the protocol stack.
		</td>
	</tr>
	<tr>
		<td>**timeout**</td>
		<td>60000</td>
		<td>Indicates the number of milliseconds allowed for JGroups operations.</td>
	</tr>
</table>

e.g.

	<ha-jdbc xmlns="urn:ha-jdbc:cluster:3.0">
		<distributable id="jgroups">
			<property name="stack">udp.xml</property>
		</distributable>
		<cluster><!-- ... --></cluster>
	</ha-jdbc>

[jgroups]: http://community.jboss.org/wiki/JGroups "JGroups"
	