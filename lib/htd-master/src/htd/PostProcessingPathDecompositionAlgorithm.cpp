/* 
 * File:   PostProcessingPathDecompositionAlgorithm.cpp
 *
 * Author: ABSEHER Michael (abseher@dbai.tuwien.ac.at)
 * 
 * Copyright 2015-2017, Michael Abseher
 *    E-Mail: <abseher@dbai.tuwien.ac.at>
 * 
 * This file is part of htd.
 * 
 * htd is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free 
 * Software Foundation, either version 3 of the License, or (at your 
 * option) any later version.
 * 
 * htd is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public 
 * License for more details.

 * You should have received a copy of the GNU General Public License
 * along with htd.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef HTD_HTD_POSTPROCESSINGPATHDECOMPOSITIONALGORITHM_CPP
#define HTD_HTD_POSTPROCESSINGPATHDECOMPOSITIONALGORITHM_CPP

#include <htd/Globals.hpp>
#include <htd/Helpers.hpp>
#include <htd/PostProcessingPathDecompositionAlgorithm.hpp>
#include <htd/PathDecompositionFactory.hpp>
#include <htd/TreeDecompositionFactory.hpp>
#include <htd/TreeDecompositionAlgorithmFactory.hpp>
#include <htd/CompressionOperation.hpp>
#include <htd/JoinNodeReplacementOperation.hpp>
#include <htd/GraphPreprocessor.hpp>

#include <cstdarg>
#include <stdexcept>
#include <vector>

/**
 *  Private implementation details of class htd::PostProcessingPathDecompositionAlgorithm.
 */
struct htd::PostProcessingPathDecompositionAlgorithm::Implementation
{
    /**
     *  Constructor for the implementation details structure.
     *
     *  @param[in] manager   The management instance to which the current object instance belongs.
     */
    Implementation(const htd::LibraryInstance * const manager) : managementInstance_(manager), labelingFunctions_(), postProcessingOperations_()
    {

    }

    virtual ~Implementation()
    {
        for (auto & labelingFunction : labelingFunctions_)
        {
            delete labelingFunction;
        }

        for (auto & postProcessingOperation : postProcessingOperations_)
        {
            delete postProcessingOperation;
        }
    }

    /**
     *  The management instance to which the current object instance belongs.
     */
    const htd::LibraryInstance * managementInstance_;

    /**
     *  The labeling functions which are applied after the decomposition was computed.
     */
    std::vector<htd::ILabelingFunction *> labelingFunctions_;

    /**
     *  The manipuation operations which are applied after the decomposition was computed.
     */
    std::vector<htd::IPathDecompositionManipulationOperation *> postProcessingOperations_;
};

htd::PostProcessingPathDecompositionAlgorithm::PostProcessingPathDecompositionAlgorithm(const htd::LibraryInstance * const manager) : implementation_(new Implementation(manager))
{

}

htd::PostProcessingPathDecompositionAlgorithm::PostProcessingPathDecompositionAlgorithm(const htd::LibraryInstance * const manager, const std::vector<htd::IDecompositionManipulationOperation *> & manipulationOperations) : implementation_(new Implementation(manager))
{
    setManipulationOperations(manipulationOperations);
}

htd::PostProcessingPathDecompositionAlgorithm::~PostProcessingPathDecompositionAlgorithm()
{

}

htd::IPathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::computeDecomposition(const htd::IMultiHypergraph & graph) const
{
    return computeDecomposition(graph, std::vector<htd::IDecompositionManipulationOperation *>());
}

htd::IPathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::computeDecomposition(const htd::IMultiHypergraph & graph, const std::vector<htd::IDecompositionManipulationOperation *> & manipulationOperations) const
{
    htd::IPathDecomposition * ret = nullptr;

    htd::GraphPreprocessor preprocessor(implementation_->managementInstance_);

    htd::IPreprocessedGraph * preprocessedGraph = preprocessor.prepare(graph);

    ret = computeDecomposition(graph, *preprocessedGraph, manipulationOperations);

    delete preprocessedGraph;

    return ret;
}

htd::IPathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::computeDecomposition(const htd::IMultiHypergraph & graph, const htd::IPreprocessedGraph & preprocessedGraph) const
{
    return computeDecomposition(graph, preprocessedGraph, std::vector<htd::IDecompositionManipulationOperation *>());
}

htd::IPathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::computeDecomposition(const htd::IMultiHypergraph & graph, const htd::IPreprocessedGraph & preprocessedGraph, const std::vector<htd::IDecompositionManipulationOperation *> & manipulationOperations) const
{
    htd::ITreeDecompositionAlgorithm * algorithm = managementInstance()->treeDecompositionAlgorithmFactory().createInstance();

    HTD_ASSERT(algorithm != nullptr)

    htd::ITreeDecomposition * treeDecomposition = algorithm->computeDecomposition(graph, preprocessedGraph);

    HTD_ASSERT(treeDecomposition != nullptr)

    delete algorithm;

    htd::IMutableTreeDecomposition & mutableTreeDecomposition =
        managementInstance()->treeDecompositionFactory().accessMutableInstance(*treeDecomposition);

    htd::CompressionOperation compressionOperation(managementInstance());

    compressionOperation.apply(graph, mutableTreeDecomposition);

    htd::JoinNodeReplacementOperation joinNodeReplacementOperation(managementInstance());

    joinNodeReplacementOperation.apply(graph, mutableTreeDecomposition);

    htd::IMutablePathDecomposition * ret = toPathDecomposition(std::move(mutableTreeDecomposition));

    delete treeDecomposition;

    std::vector<htd::ILabelingFunction *> labelingFunctions;

    std::vector<htd::IPathDecompositionManipulationOperation *> postProcessingOperations;

    for (htd::IDecompositionManipulationOperation * operation : manipulationOperations)
    {
        htd::ILabelingFunction * labelingFunction = dynamic_cast<htd::ILabelingFunction *>(operation);

        if (labelingFunction != nullptr)
        {
            labelingFunctions.push_back(labelingFunction);
        }

        htd::IPathDecompositionManipulationOperation * manipulationOperation = dynamic_cast<htd::IPathDecompositionManipulationOperation *>(operation);

        if (manipulationOperation != nullptr)
        {
            postProcessingOperations.push_back(manipulationOperation);
        }
    }

    for (const auto & operation : implementation_->postProcessingOperations_)
    {
        operation->apply(graph, *ret);
    }

    for (const auto & operation : postProcessingOperations)
    {
        operation->apply(graph, *ret);
    }

    for (const auto & labelingFunction : implementation_->labelingFunctions_)
    {
        for (htd::vertex_t vertex : ret->vertices())
        {
            htd::ILabelCollection * labelCollection = ret->labelings().exportVertexLabelCollection(vertex);

            htd::ILabel * newLabel = labelingFunction->computeLabel(ret->bagContent(vertex), *labelCollection);

            delete labelCollection;

            ret->setVertexLabel(labelingFunction->name(), vertex, newLabel);
        }
    }

    for (const auto & labelingFunction : labelingFunctions)
    {
        for (htd::vertex_t vertex : ret->vertices())
        {
            htd::ILabelCollection * labelCollection = ret->labelings().exportVertexLabelCollection(vertex);

            htd::ILabel * newLabel = labelingFunction->computeLabel(ret->bagContent(vertex), *labelCollection);

            delete labelCollection;

            ret->setVertexLabel(labelingFunction->name(), vertex, newLabel);
        }
    }

    for (auto & labelingFunction : labelingFunctions)
    {
        delete labelingFunction;
    }

    for (auto & postProcessingOperation : postProcessingOperations)
    {
        delete postProcessingOperation;
    }

    return ret;
}

htd::IPathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::computeDecomposition(const htd::IMultiHypergraph & graph, int manipulationOperationCount, ...) const
{
    va_list arguments;

    va_start(arguments, manipulationOperationCount);

    std::vector<htd::IDecompositionManipulationOperation *> manipulationOperations;

    for (int manipulationOperationIndex = 0; manipulationOperationIndex < manipulationOperationCount; manipulationOperationIndex++)
    {
        manipulationOperations.push_back(va_arg(arguments, htd::IDecompositionManipulationOperation *));
    }

    va_end(arguments);

    return computeDecomposition(graph, manipulationOperations);
}

htd::IPathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::computeDecomposition(const htd::IMultiHypergraph & graph, const htd::IPreprocessedGraph & preprocessedGraph, int manipulationOperationCount, ...) const
{
    va_list arguments;

    va_start(arguments, manipulationOperationCount);

    std::vector<htd::IDecompositionManipulationOperation *> manipulationOperations;

    for (int manipulationOperationIndex = 0; manipulationOperationIndex < manipulationOperationCount; manipulationOperationIndex++)
    {
        manipulationOperations.push_back(va_arg(arguments, htd::IDecompositionManipulationOperation *));
    }

    va_end(arguments);

    return computeDecomposition(graph, preprocessedGraph, manipulationOperations);
}

htd::IMutablePathDecomposition * htd::PostProcessingPathDecompositionAlgorithm::toPathDecomposition(htd::IMutableTreeDecomposition && decomposition) const
{
    htd::IMutablePathDecomposition * ret =
        managementInstance()->pathDecompositionFactory().createInstance();

    if (decomposition.vertexCount() > 0)
    {
        const htd::ConstCollection<std::string> & labelNames = decomposition.labelNames();

        htd::vertex_t currentVertex = decomposition.root();

        htd::vertex_t newVertex = ret->insertRoot(std::move(decomposition.mutableBagContent(currentVertex)),
                                                  std::move(decomposition.mutableInducedHyperedges(currentVertex)));

        for (const std::string & labelName : labelNames)
        {
            if (decomposition.isLabeledVertex(labelName, currentVertex))
            {
                ret->setVertexLabel(labelName, newVertex, decomposition.vertexLabel(labelName, currentVertex).clone());
            }
        }

        while (decomposition.childCount(currentVertex) == 1)
        {
            currentVertex = decomposition.childAtPosition(currentVertex, 0);

            newVertex = ret->addChild(newVertex,
                                      std::move(decomposition.mutableBagContent(currentVertex)),
                                      std::move(decomposition.mutableInducedHyperedges(currentVertex)));

            for (const std::string & labelName : labelNames)
            {
                if (decomposition.isLabeledVertex(labelName, currentVertex))
                {
                    ret->setVertexLabel(labelName, newVertex, decomposition.vertexLabel(labelName, currentVertex).clone());
                }
            }
        }

        HTD_ASSERT(decomposition.childCount(currentVertex) == 0)
    }

    return ret;
}

void htd::PostProcessingPathDecompositionAlgorithm::setManipulationOperations(const std::vector<htd::IDecompositionManipulationOperation *> & manipulationOperations)
{
    for (auto & labelingFunction : implementation_->labelingFunctions_)
    {
        delete labelingFunction;
    }

    for (auto & postProcessingOperation : implementation_->postProcessingOperations_)
    {
        delete postProcessingOperation;
    }

    implementation_->labelingFunctions_.clear();

    implementation_->postProcessingOperations_.clear();

    addManipulationOperations(manipulationOperations);
}

void htd::PostProcessingPathDecompositionAlgorithm::addManipulationOperation(htd::IDecompositionManipulationOperation * manipulationOperation)
{
    bool assigned = false;

    htd::ILabelingFunction * labelingFunction = dynamic_cast<htd::ILabelingFunction *>(manipulationOperation);

    if (labelingFunction != nullptr)
    {
        implementation_->labelingFunctions_.push_back(labelingFunction);

        assigned = true;
    }

    htd::IPathDecompositionManipulationOperation * newManipulationOperation = dynamic_cast<htd::IPathDecompositionManipulationOperation *>(manipulationOperation);

    if (newManipulationOperation != nullptr)
    {
        implementation_->postProcessingOperations_.push_back(newManipulationOperation);

        assigned = true;
    }

    if (!assigned)
    {
        delete manipulationOperation;
    }
}

void htd::PostProcessingPathDecompositionAlgorithm::addManipulationOperations(const std::vector<htd::IDecompositionManipulationOperation *> & manipulationOperations)
{
    for (htd::IDecompositionManipulationOperation * operation : manipulationOperations)
    {
        addManipulationOperation(operation);
    }
}

bool htd::PostProcessingPathDecompositionAlgorithm::isSafelyInterruptible(void) const
{
    return false;
}

const htd::LibraryInstance * htd::PostProcessingPathDecompositionAlgorithm::managementInstance(void) const HTD_NOEXCEPT
{
    return implementation_->managementInstance_;
}

void htd::PostProcessingPathDecompositionAlgorithm::setManagementInstance(const htd::LibraryInstance * const manager)
{
    HTD_ASSERT(manager != nullptr)

    implementation_->managementInstance_ = manager;
}

htd::PostProcessingPathDecompositionAlgorithm * htd::PostProcessingPathDecompositionAlgorithm::clone(void) const
{
    htd::PostProcessingPathDecompositionAlgorithm * ret = new htd::PostProcessingPathDecompositionAlgorithm(managementInstance());

    for (const auto & labelingFunction : implementation_->labelingFunctions_)
    {
#ifndef HTD_USE_VISUAL_STUDIO_COMPATIBILITY_MODE
        ret->addManipulationOperation(labelingFunction->clone());
#else
        ret->addManipulationOperation(labelingFunction->cloneLabelingFunction());
#endif
    }

    for (const auto & postProcessingOperation : implementation_->postProcessingOperations_)
    {
#ifndef HTD_USE_VISUAL_STUDIO_COMPATIBILITY_MODE
        ret->addManipulationOperation(postProcessingOperation->clone());
#else
        ret->addManipulationOperation(postProcessingOperation->clonePathDecompositionManipulationOperation());
#endif
    }

    ret->setManagementInstance(managementInstance());

    return ret;
}

#endif /* HTD_HTD_POSTPROCESSINGPATHDECOMPOSITIONALGORITHM_CPP */
