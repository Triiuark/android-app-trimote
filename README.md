# Trimote

Trimote is an android remote app.

## Configuration
```xml
<?xml version="1.0" encoding="UTF-8"?>
<remote version="1.0">
	<page label="TV">
		<row>
			<button>
				<actions><!-- turn backlight on Philips TV off -->
					<ir type="RC5" address="0000" value="108" wait="000"/><!-- green -->
					<ir type="RC5" address="0000" value="081" wait="500"/><!-- down -->
					<ir type="RC5" address="0000" value="087" wait="050"/><!-- ok -->
					<ir type="RC5" address="0000" value="087" wait="100"/><!-- ok -->
				</actions>
				<text>e++</text>
			</button>
		</row>
	</page>
</remote>
```

