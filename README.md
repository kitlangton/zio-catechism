# Running

Install npm dependencies:

```
npm install
```

Build the front-end:

```
sbt frontendJS/fastOptJS
```

Start the webpack dev server:

```
npm start
```

## Open 

Open http://localhost:30090/ in the browser.

# Developing

To make sbt re-compile the front-end on code changes:

```
sbt ~frontendJS/fastOptJS
```

# Prod build

Build an optimized js for the front-end:

```
sbt frontendJS/fullOptJS
```

Run the npm:

```
npm run build:prod
```

The front end assets will be generated into the `dist` folder.