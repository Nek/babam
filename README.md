# BabaAstro

A minimal clone of [Astro](https://astro.build) built with Clojure and Babashka. Build fast, content-focused websites with the power of Clojure.

## Roadmap

- cljc+html based components
- resolve defs in a component to {{name}} vars in html
- 

## Quick Start

```bash
# Create a new project
bb create-baba-astro my-site

# Change into project directory
cd my-site

# Start the dev server
bb dev
```

## Project Structure

```
my-site/
├── src/
│   └── pages/
│       └── index.clj
├── public/
│   └── assets/
├── bb.edn
└── README.md
```

## Documentation

- [Getting Started](docs/getting-started.md)
- [Project Structure](docs/structure.md)
- [Components](docs/components.md)
- [Routing](docs/routing.md)
- [Markdown](docs/markdown.md)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see [LICENSE](LICENSE) for details
