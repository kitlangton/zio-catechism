# ZIO Catechism

## Overview

#### [https://zio.surge.sh](https://zio.surge.sh)

As a form of interactive documentation, this project aims to help its readers build an intuitive understanding of ZIO effects and combinators.
It does this by simulating the ZIO effect execution visually.

## Tech
- [ZIO](https://github.com/zio/zio) — Amongst other things, we use ZIO to simulate ZIO. It's perfect for that.
- [Scala.js](https://www.scala-js.org) — Write your web apps in Scala! ZIO works with it out-of-the-box.
- [Laminar](https://github.com/raquo/Laminar) — My favorite way to write frontends. An FRP UI framework for ScalaJS.
- Animator? (WIP) — An FRP-based animation library I'm working on. For now, it exists within this repo. It does all the animating.

## Contribution

If you think visualizing stuff is cool and want to contribute, feel free to open a PR and/or issue. If you'd like some 
help getting started, I'm more than happy to pair on the weekends.

Even simply opening issues with content suggestions would be tremendously helpful: What ZIO concepts have you had the most 
difficultly in coming to understand? What do you think would look "cool"?

### Roadmap

The first goal should be to add a lot more content, prioritizing those aspects of ZIO which may be less immediately 
graspable, as well as those which would benefit most from visualization. 

It's also important to figure out the simplest way of representing each concept—just as good library design requires 
orthogonality of its primitives, each simulation should ideally be as minimal and orthogonal as possible. Though in
some cases, it might be difficult to represent one concept by itself; e.g., `forever` might be better visualized when
coupled with `race`. In these cases, it would be best to try to order the content such that concepts without such
*dependencies* occur before their dependents.

#### Combinators
- [x] collectAll
- [x] collectAllPar
- [x] collectAllParN
- [x] foreach
- [x] foreachPar
- [x] foreach_
- [x] foreachPar_
- [ ] reduceAll
- [ ] mergeAll
- [x] fork
- [x] forkDaemon
- [x] race
- [x] join
- [ ] forever
- [ ] Fiber.interrupt
- [ ] orElse
- [ ] orElseEither
- [ ] zip
- [ ] zipPar
- [ ] timeout
- [ ] ensuring
- [ ] bracket
- [ ] ...and many more

#### Other
- [ ] ZIO STM
- [ ] ZLayer
- [ ] ZStream
- [ ] Queue
- [ ] Ref
- [ ] ZManaged

#### UI
- [ ] Create a navigation/search interface for better organization

# Development

## Running

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

### Open 

Open http://localhost:30090/ in the browser.

## Developing

To make sbt re-compile the front-end on code changes:

```
sbt ~frontendJS/fastOptJS
```

## Prod build

Build an optimized js for the front-end:

```
sbt frontendJS/fullOptJS
```

Run the npm:

```
npm run build:prod
```

The front end assets will be generated into the `dist` folder.