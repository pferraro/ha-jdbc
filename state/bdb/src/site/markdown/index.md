### State management: BerkeleyDB provider

A persistent state manager that uses a BerkeleyDB database.

This provider supports the following properties, in addition to properties to manipulate connection pooling behavior.
The complete list of pooling properties and their default values are available in the [Apache Commons Pool documentation][commons-pool] documentation..

<table>
	<tr>
		<th>Property</th>
		<th>Default</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>**locationPattern**</td>
		<td>
			{1}/{0}
		</td>
		<td>
			A MessageFormat pattern indicating the base location of the embedded database.
			The pattern can accept 2 parameters:
			<ol>
				<li>The cluster identifier</li>
				<li>`$HOME/.ha-jdbc`</li>
			</ol>
		</td>
	</tr>
</table>

e.g.

	<ha-jdbc xmlns="urn:ha-jdbc:cluster:3.0">
		<state id="berkeleydb">
			<property name="locationPattern">/tmp/{0}</property>
		</state>
		<cluster><!-- ... --></cluster>
	</ha-jdbc>
